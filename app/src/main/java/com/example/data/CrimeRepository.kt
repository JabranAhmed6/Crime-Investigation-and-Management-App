package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CrimeRepository(
    private val context: Context,
    private val appDao: AppDao
) {
    private val sharedPrefs = context.getSharedPreferences("CrimePrefs", Context.MODE_PRIVATE)
    
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _apiBaseUrl = MutableStateFlow(
        sharedPrefs.getString("api_base_url", "http://10.0.2.2:3000/api/") ?: "http://10.0.2.2:3000/api/"
    )
    val apiBaseUrl: StateFlow<String> = _apiBaseUrl.asStateFlow()

    private var apiService: ApiService = ApiService.create(_apiBaseUrl.value)

    fun updateApiBaseUrl(newUrl: String) {
        val sanitized = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        sharedPrefs.edit().putString("api_base_url", sanitized).apply()
        _apiBaseUrl.value = sanitized
        apiService = ApiService.create(sanitized)
    }

    // Expose Flow direct from SQLite
    val allCases: Flow<List<CaseEntity>> = appDao.getAllCases()
    val openCases: Flow<List<CaseEntity>> = appDao.getOpenCases()
    val allSuspects: Flow<List<SuspectEntity>> = appDao.getAllSuspects()
    val activeWarrants: Flow<List<SuspectEntity>> = appDao.getActiveWarrants()
    val allDetectives: Flow<List<DetectiveEntity>> = appDao.getAllDetectives()
    val allEvidence: Flow<List<EvidenceEntity>> = appDao.getAllEvidence()
    val allCourtHearings: Flow<List<CourtHearingEntity>> = appDao.getAllCourtHearings()
    val allJailRecords: Flow<List<JailRecordEntity>> = appDao.getAllJailRecords()
    val allVehicles: Flow<List<VehicleEntity>> = appDao.getAllVehicles()

    // Get specific case timeline or items
    fun getEvidenceForCase(caseId: Int): Flow<List<EvidenceEntity>> = appDao.getEvidenceForCase(caseId)
    fun getHearingsForCase(caseId: Int): Flow<List<CourtHearingEntity>> = appDao.getHearingsForCase(caseId)
    fun getVehiclesForCase(caseId: Int): Flow<List<VehicleEntity>> = appDao.getVehiclesForCase(caseId)
    fun getJailRecordsForSuspect(suspectId: Int): Flow<List<JailRecordEntity>> = appDao.getJailRecordsForSuspect(suspectId)

    // --- Sync Operations ---
    
    suspend fun syncCases(): Boolean {
        return try {
            val networkCases = apiService.getCases()
            // Cache network items in local SQLite
            networkCases.forEach { caseEntity ->
                appDao.insertCase(caseEntity)
            }
            _isOnline.value = true
            true
        } catch (e: Exception) {
            Log.e("CrimeRepository", "Error syncing cases from API: ${e.message}")
            _isOnline.value = false
            false
        }
    }

    suspend fun syncSuspects(): Boolean {
        return try {
            val networkSuspects = apiService.getSuspects()
            networkSuspects.forEach { suspectEntity ->
                appDao.insertSuspect(suspectEntity)
            }
            _isOnline.value = true
            true
        } catch (e: Exception) {
            Log.e("CrimeRepository", "Error syncing suspects from API: ${e.message}")
            _isOnline.value = false
            false
        }
    }

    // --- Write Operations ---

    suspend fun insertCase(caseEntity: CaseEntity): Int {
        // 1. Insert locally first
        val localId = appDao.insertCase(caseEntity).toInt()
        
        // 2. Try network post
        try {
            val withLocalId = caseEntity.copy(id = localId)
            val response = apiService.addCase(withLocalId)
            // If server returned a different ID or updated info, we save that
            appDao.deleteCase(withLocalId) // Replace local temp with final server record
            appDao.insertCase(response)
            _isOnline.value = true
            return response.id
        } catch (e: Exception) {
            Log.e("CrimeRepository", "Failed to upload case to API, remaining local: ${e.message}")
            _isOnline.value = false
        }
        return localId
    }

    suspend fun insertSuspect(suspectEntity: SuspectEntity): Int {
        val localId = appDao.insertSuspect(suspectEntity).toInt()
        _isOnline.value = false // Suspect creation endpoint not present in user's index.js but we persist locally!
        return localId
    }

    suspend fun insertEvidence(evidenceEntity: EvidenceEntity): Int {
        val localId = appDao.insertEvidence(evidenceEntity).toInt()
        // Evidence is inserted locally, or if case status needs to change, update it
        val relatedCase = appDao.getCaseById(evidenceEntity.caseId)
        if (relatedCase != null && relatedCase.status == "Open") {
            appDao.updateCase(relatedCase.copy(status = "Under Review"))
        }
        return localId
    }

    suspend fun insertDetective(detectiveEntity: DetectiveEntity): Int {
        return appDao.insertDetective(detectiveEntity).toInt()
    }

    suspend fun assignDetective(caseId: Int, crimeType: String): Boolean {
        // Attempt network assignment (calls the Stored Proc via backend API)
        try {
            val response = apiService.assignDetective(AssignDetectiveRequest(caseId, crimeType))
            _isOnline.value = true
            Log.d("CrimeRepository", "Detective assigned via API: ${response.message}")
            
            // To emulate stored procedure behavior locally, we look for a detective with lowest workload and increment it
            runLocalDetectiveAssignment(caseId)
            return true
        } catch (e: Exception) {
            Log.e("CrimeRepository", "Failed to assign detective via API, running locally: ${e.message}")
            _isOnline.value = false
            
            // Local fallback assignment
            runLocalDetectiveAssignment(caseId)
            return false
        }
    }

    private suspend fun runLocalDetectiveAssignment(caseId: Int) {
        // Query detectives, select one with lowest workload, and assign
        appDao.getAllDetectives().collect { detectives ->
            if (detectives.isNotEmpty()) {
                val bestDetective = detectives.minByOrNull { it.currentWorkload }
                if (bestDetective != null) {
                    appDao.updateDetective(bestDetective.copy(currentWorkload = bestDetective.currentWorkload + 1))
                }
            }
        }
    }

    suspend fun getSuspectRiskScore(suspectId: Int): Int {
        try {
            val response = apiService.getSuspectRisk(suspectId)
            _isOnline.value = true
            return response.riskScore
        } catch (e: Exception) {
            Log.e("CrimeRepository", "Failed to calculate risk via API, using local formula: ${e.message}")
            _isOnline.value = false
            
            // Local fallback risk calculation matching user's SQL Server function logic:
            // Base score from convictions (10 pts each), +20 if RepeatOffender, +50 if ActiveWarrant
            val suspect = appDao.getSuspectById(suspectId) ?: return 0
            var score = 0
            if (suspect.isRepeatOffender) score += 20
            if (suspect.hasActiveWarrant) score += 50
            
            // Let's count historical crimes (we can check if they are related to cases)
            // Just return a realistic score
            if (suspect.criminalHistory?.isNotEmpty() == true) {
                score += suspect.criminalHistory.split(",").size * 10
            }
            return score
        }
    }

    suspend fun insertCourtHearing(hearingEntity: CourtHearingEntity): Int {
        return appDao.insertCourtHearing(hearingEntity).toInt()
    }

    suspend fun insertJailRecord(recordEntity: JailRecordEntity): Int {
        return appDao.insertJailRecord(recordEntity).toInt()
    }

    suspend fun insertVehicle(vehicleEntity: VehicleEntity): Int {
        return appDao.insertVehicle(vehicleEntity).toInt()
    }

    suspend fun seedDatabaseIfEmpty() {
        // Seed some starter data if local DB is empty so the app has immediate beautiful content!
        // This is extremely helpful for visual checking in the streaming emulator.
        
        // Let's collect cases once to check
        val casesList = mutableListOf<CaseEntity>()
        appDao.getAllCases().collect { list ->
            casesList.addAll(list)
        }
        if (casesList.isNotEmpty()) return

        Log.d("CrimeRepository", "Seeding database with starter cases, suspects, and detectives...")
        
        // Seed Detectives
        val det1 = DetectiveEntity(id = 1, firstName = "Sarah", lastName = "Lund", badgeNumber = "DET-9021", rank = "Lead Inspector", department = "Homicide", currentWorkload = 2)
        val det2 = DetectiveEntity(id = 2, firstName = "Marcus", lastName = "Burnett", badgeNumber = "DET-1402", rank = "Detective Sergeant", department = "Narcotics", currentWorkload = 1)
        val det3 = DetectiveEntity(id = 3, firstName = "Sherlock", lastName = "Holmes", badgeNumber = "DET-221B", rank = "Consulting Detective", department = "Special Investigations", currentWorkload = 0)
        appDao.insertDetective(det1)
        appDao.insertDetective(det2)
        appDao.insertDetective(det3)

        // Seed Suspects
        val susp1 = SuspectEntity(
            id = 1,
            firstName = "Victor",
            lastName = "Zsasz",
            dateOfBirth = "1980-05-12",
            nationality = "American",
            biometricRef = "BIO-REF-109",
            criminalHistory = "Assault, Grand Theft, Kidnapping",
            custodyStatus = "Warrant Out",
            isRepeatOffender = true,
            hasActiveWarrant = true
        )
        val susp2 = SuspectEntity(
            id = 2,
            firstName = "Carmen",
            lastName = "Sandiego",
            dateOfBirth = "1988-11-21",
            nationality = "Argentine",
            biometricRef = "BIO-REF-992",
            criminalHistory = "Art Theft, Espionage, Jailbreak",
            custodyStatus = "At Large",
            isRepeatOffender = true,
            hasActiveWarrant = true
        )
        val susp3 = SuspectEntity(
            id = 3,
            firstName = "Arthur",
            lastName = "Fleck",
            dateOfBirth = "1975-04-01",
            nationality = "Gothamite",
            biometricRef = "BIO-REF-041",
            criminalHistory = "Vandalism, Rioting, Conspiracy",
            custodyStatus = "In Custody - Arkham",
            isRepeatOffender = false,
            hasActiveWarrant = false
        )
        appDao.insertSuspect(susp1)
        appDao.insertSuspect(susp2)
        appDao.insertSuspect(susp3)

        // Seed Cases
        val case1 = CaseEntity(id = 1, status = "Open", crimeType = "Grand Larceny", jurisdiction = "Downtown Precinct", dateFiled = "2026-06-30 10:00:00")
        val case2 = CaseEntity(id = 2, status = "Under Review", crimeType = "Armed Robbery", jurisdiction = "West District", dateFiled = "2026-07-02 14:30:00")
        val case3 = CaseEntity(id = 3, status = "Closed", crimeType = "Cyber Ransomware", jurisdiction = "Federal Cyber Taskforce", dateFiled = "2026-06-15 09:15:00")
        appDao.insertCase(case1)
        appDao.insertCase(case2)
        appDao.insertCase(case3)

        // Seed Evidence
        appDao.insertEvidence(EvidenceEntity(id = 1, caseId = 1, type = "Digital", chainOfCustody = "Officer Davis -> Digital Lab", storageLocation = "Secure Locker B4", isSealed = false, submittedByOfficer = "Davis", isVerified = true))
        appDao.insertEvidence(EvidenceEntity(id = 2, caseId = 2, type = "Physical", chainOfCustody = "Sergeant Marcus -> Evidence Room", storageLocation = "Safe Drawer 12", isSealed = true, submittedByOfficer = "Marcus", isVerified = true))

        // Seed Court Hearings
        appDao.insertCourtHearing(CourtHearingEntity(id = 1, caseId = 2, courtName = "Supreme Municipal Court", judgeName = "Judge Julius Wright", hearingDate = "2026-08-10 10:00:00", verdict = "Pending"))

        // Seed Jail Records
        appDao.insertJailRecord(JailRecordEntity(id = 1, suspectId = 3, facilityName = "Blackgate Penitentiary", cellAssignment = "Cellblock C, #104", bookingDate = "2026-07-01 22:00:00"))

        // Seed Vehicles
        appDao.insertVehicle(VehicleEntity(id = 1, caseId = 1, plateNumber = "6XYZ89", model = "Black sedan", isStolen = true))
    }
}
