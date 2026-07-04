package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CrimeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = CrimeRepository(application, database.appDao())

    // Base URL and Sync State
    val apiBaseUrl: StateFlow<String> = repository.apiBaseUrl
    val isOnline: StateFlow<Boolean> = repository.isOnline

    // local lists observed reactively
    val allCases: StateFlow<List<CaseEntity>> = repository.allCases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val openCases: StateFlow<List<CaseEntity>> = repository.openCases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSuspects: StateFlow<List<SuspectEntity>> = repository.allSuspects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeWarrants: StateFlow<List<SuspectEntity>> = repository.activeWarrants
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDetectives: StateFlow<List<DetectiveEntity>> = repository.allDetectives
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEvidence: StateFlow<List<EvidenceEntity>> = repository.allEvidence
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active detail states
    private val _selectedCaseId = MutableStateFlow<Int?>(null)
    val selectedCaseId: StateFlow<Int?> = _selectedCaseId.asStateFlow()

    val selectedCase: StateFlow<CaseEntity?> = _selectedCaseId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.allCases.map { list -> list.find { it.id == id } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedCaseEvidence: StateFlow<List<EvidenceEntity>> = _selectedCaseId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getEvidenceForCase(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedCaseHearings: StateFlow<List<CourtHearingEntity>> = _selectedCaseId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getHearingsForCase(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedCaseVehicles: StateFlow<List<VehicleEntity>> = _selectedCaseId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getVehiclesForCase(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Analysis State
    private val _aiAnalysisState = MutableStateFlow<AiAnalysisState>(AiAnalysisState.Idle)
    val aiAnalysisState: StateFlow<AiAnalysisState> = _aiAnalysisState.asStateFlow()

    // Suspect Risk Score State
    private val _suspectRiskScores = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val suspectRiskScores: StateFlow<Map<Int, Int>> = _suspectRiskScores.asStateFlow()

    // Operation status notification (Toast or Snackbar triggers)
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        // Seeding initial data on startup to ensure a gorgeous, fully-featured app immediately
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
            // Try to fetch initial cases and suspects from network API
            syncData()
        }
    }

    fun selectCase(caseId: Int?) {
        _selectedCaseId.value = caseId
        _aiAnalysisState.value = AiAnalysisState.Idle
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun updateServerUrl(url: String) {
        repository.updateApiBaseUrl(url)
        _statusMessage.value = "Server endpoint updated."
        syncData()
    }

    fun syncData() {
        viewModelScope.launch {
            val casesSynced = repository.syncCases()
            val suspectsSynced = repository.syncSuspects()
            if (casesSynced && suspectsSynced) {
                _statusMessage.value = "Synced with SQL Server successfully."
            } else {
                _statusMessage.value = "Sync failed. Running in Local Database mode."
            }
        }
    }

    fun createCase(crimeType: String, jurisdiction: String) {
        viewModelScope.launch {
            val newCase = CaseEntity(
                crimeType = crimeType,
                jurisdiction = jurisdiction,
                status = "Open",
                dateFiled = getCurrentTimestamp()
            )
            val insertedId = repository.insertCase(newCase)
            _statusMessage.value = "Case #$insertedId filed successfully."
        }
    }

    fun createSuspect(firstName: String, lastName: String, nationality: String, dob: String, criminalHistory: String, activeWarrant: Boolean) {
        viewModelScope.launch {
            val newSuspect = SuspectEntity(
                firstName = firstName,
                lastName = lastName,
                nationality = nationality,
                dateOfBirth = dob,
                criminalHistory = criminalHistory,
                custodyStatus = if (activeWarrant) "Warrant Out" else "At Large",
                hasActiveWarrant = activeWarrant,
                isRepeatOffender = criminalHistory.split(",").size >= 2,
                updatedAt = getCurrentTimestamp()
            )
            val insertedId = repository.insertSuspect(newSuspect)
            _statusMessage.value = "Suspect #$insertedId added to database."
        }
    }

    fun addEvidence(caseId: Int, type: String, storageLocation: String, chainOfCustody: String, submittedBy: String) {
        viewModelScope.launch {
            val evidence = EvidenceEntity(
                caseId = caseId,
                type = type,
                storageLocation = storageLocation,
                chainOfCustody = chainOfCustody,
                submittedByOfficer = submittedBy,
                submissionDate = getCurrentTimestamp(),
                isVerified = true
            )
            repository.insertEvidence(evidence)
            _statusMessage.value = "$type evidence logged successfully."
        }
    }

    fun assignDetectiveToCase(caseId: Int, crimeType: String) {
        viewModelScope.launch {
            _statusMessage.value = "Assigning detective..."
            val onlineSuccess = repository.assignDetective(caseId, crimeType)
            if (onlineSuccess) {
                _statusMessage.value = "Detective assigned and synced with API."
            } else {
                _statusMessage.value = "Detective assigned (Local Offline Database)."
            }
        }
    }

    fun calculateSuspectRisk(suspectId: Int) {
        viewModelScope.launch {
            val score = repository.getSuspectRiskScore(suspectId)
            val currentScores = _suspectRiskScores.value.toMutableMap()
            currentScores[suspectId] = score
            _suspectRiskScores.value = currentScores
            _statusMessage.value = "Risk Score calculated."
        }
    }

    fun runAiCaseAnalysis(caseEntity: CaseEntity) {
        viewModelScope.launch {
            _aiAnalysisState.value = AiAnalysisState.Loading
            
            // Gather suspects info string
            val suspectsList = allSuspects.value
            val suspectsStr = suspectsList.joinToString("\n") { 
                "- ${it.firstName} ${it.lastName} (${it.nationality}), Warrant: ${it.hasActiveWarrant}, Custody: ${it.custodyStatus}" 
            }.ifEmpty { "No specific suspects documented." }

            // Gather evidence info string
            val evidenceList = selectedCaseEvidence.value
            val evidenceStr = evidenceList.joinToString("\n") {
                "- Type: ${it.type}, Location: ${it.storageLocation}, Custody: ${it.chainOfCustody}"
            }.ifEmpty { "No evidence gathered yet." }

            // Gather timeline
            val hearingsList = selectedCaseHearings.value
            val vehiclesList = selectedCaseVehicles.value
            val timelineStr = buildString {
                appendLine("- Case filed: ${caseEntity.dateFiled ?: "N/A"}")
                vehiclesList.forEach {
                    appendLine("- Vehicle involved: ${it.model} (${it.plateNumber}), Stolen: ${it.isStolen}")
                }
                hearingsList.forEach {
                    appendLine("- Court Hearing scheduled: ${it.hearingDate} in ${it.courtName} (Judge ${it.judgeName})")
                }
            }

            val analysisResult = GeminiService.analyzeCase(
                caseType = caseEntity.crimeType,
                status = caseEntity.status,
                jurisdiction = caseEntity.jurisdiction,
                suspects = suspectsStr,
                evidence = evidenceStr,
                timeline = timelineStr
            )

            _aiAnalysisState.value = AiAnalysisState.Success(analysisResult)
        }
    }

    private fun getCurrentTimestamp(): String {
        val d = java.util.Date()
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return format.format(d)
    }
}

sealed class AiAnalysisState {
    object Idle : AiAnalysisState()
    object Loading : AiAnalysisState()
    data class Success(val report: String) : AiAnalysisState()
}
