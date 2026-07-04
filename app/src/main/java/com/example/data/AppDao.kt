package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- Cases ---
    @Query("SELECT * FROM cases ORDER BY id DESC")
    fun getAllCases(): Flow<List<CaseEntity>>

    @Query("SELECT * FROM cases WHERE status IN ('Open', 'Under Review') ORDER BY id DESC")
    fun getOpenCases(): Flow<List<CaseEntity>>

    @Query("SELECT * FROM cases WHERE id = :caseId")
    suspend fun getCaseById(caseId: Int): CaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCase(caseEntity: CaseEntity): Long

    @Update
    suspend fun updateCase(caseEntity: CaseEntity)

    @Delete
    suspend fun deleteCase(caseEntity: CaseEntity)

    @Query("DELETE FROM cases")
    suspend fun deleteAllCases()


    // --- Suspects ---
    @Query("SELECT * FROM suspects ORDER BY id DESC")
    fun getAllSuspects(): Flow<List<SuspectEntity>>

    @Query("SELECT * FROM suspects WHERE hasActiveWarrant = 1 ORDER BY id DESC")
    fun getActiveWarrants(): Flow<List<SuspectEntity>>

    @Query("SELECT * FROM suspects WHERE id = :suspectId")
    suspend fun getSuspectById(suspectId: Int): SuspectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuspect(suspectEntity: SuspectEntity): Long

    @Update
    suspend fun updateSuspect(suspectEntity: SuspectEntity)

    @Delete
    suspend fun deleteSuspect(suspectEntity: SuspectEntity)

    @Query("DELETE FROM suspects")
    suspend fun deleteAllSuspects()


    // --- Evidence ---
    @Query("SELECT * FROM evidence ORDER BY id DESC")
    fun getAllEvidence(): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM evidence WHERE caseId = :caseId ORDER BY id DESC")
    fun getEvidenceForCase(caseId: Int): Flow<List<EvidenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvidence(evidenceEntity: EvidenceEntity): Long

    @Update
    suspend fun updateEvidence(evidenceEntity: EvidenceEntity)

    @Delete
    suspend fun deleteEvidence(evidenceEntity: EvidenceEntity)

    @Query("DELETE FROM evidence")
    suspend fun deleteAllEvidence()


    // --- Detectives ---
    @Query("SELECT * FROM detectives ORDER BY id DESC")
    fun getAllDetectives(): Flow<List<DetectiveEntity>>

    @Query("SELECT * FROM detectives WHERE id = :detectiveId")
    suspend fun getDetectiveById(detectiveId: Int): DetectiveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetective(detectiveEntity: DetectiveEntity): Long

    @Update
    suspend fun updateDetective(detectiveEntity: DetectiveEntity)

    @Query("DELETE FROM detectives")
    suspend fun deleteAllDetectives()


    // --- Court Hearings ---
    @Query("SELECT * FROM court_hearings ORDER BY id DESC")
    fun getAllCourtHearings(): Flow<List<CourtHearingEntity>>

    @Query("SELECT * FROM court_hearings WHERE caseId = :caseId ORDER BY id DESC")
    fun getHearingsForCase(caseId: Int): Flow<List<CourtHearingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourtHearing(courtHearingEntity: CourtHearingEntity): Long


    // --- Jail Records ---
    @Query("SELECT * FROM jail_records ORDER BY id DESC")
    fun getAllJailRecords(): Flow<List<JailRecordEntity>>

    @Query("SELECT * FROM jail_records WHERE suspectId = :suspectId ORDER BY id DESC")
    fun getJailRecordsForSuspect(suspectId: Int): Flow<List<JailRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJailRecord(jailRecordEntity: JailRecordEntity): Long


    // --- Vehicles ---
    @Query("SELECT * FROM vehicles ORDER BY id DESC")
    fun getAllVehicles(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE caseId = :caseId ORDER BY id DESC")
    fun getVehiclesForCase(caseId: Int): Flow<List<VehicleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicleEntity: VehicleEntity): Long
}
