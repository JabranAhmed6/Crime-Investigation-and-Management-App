package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "cases")
data class CaseEntity(
    @PrimaryKey(autoGenerate = true)
    @Json(name = "CaseID") val id: Int = 0,
    @Json(name = "Status") val status: String = "Open",
    @Json(name = "DateFiled") val dateFiled: String? = null,
    @Json(name = "CrimeType") val crimeType: String,
    @Json(name = "Jurisdiction") val jurisdiction: String,
    @Json(name = "UpdatedAt") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "suspects")
data class SuspectEntity(
    @PrimaryKey(autoGenerate = true)
    @Json(name = "SuspectID") val id: Int = 0,
    @Json(name = "FirstName") val firstName: String,
    @Json(name = "LastName") val lastName: String,
    @Json(name = "DateOfBirth") val dateOfBirth: String? = null,
    @Json(name = "Nationality") val nationality: String? = null,
    @Json(name = "BiometricRef") val biometricRef: String? = null,
    @Json(name = "CriminalHistory") val criminalHistory: String? = null,
    @Json(name = "CustodyStatus") val custodyStatus: String? = null,
    @Json(name = "IsRepeatOffender") val isRepeatOffender: Boolean = false,
    @Json(name = "HasActiveWarrant") val hasActiveWarrant: Boolean = false,
    @Json(name = "UpdatedAt") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "evidence")
data class EvidenceEntity(
    @PrimaryKey(autoGenerate = true)
    @Json(name = "EvidenceID") val id: Int = 0,
    @Json(name = "CaseID") val caseId: Int,
    @Json(name = "Type") val type: String, // Physical, Digital, Forensic
    @Json(name = "ChainOfCustody") val chainOfCustody: String? = null,
    @Json(name = "StorageLocation") val storageLocation: String? = null,
    @Json(name = "IsSealed") val isSealed: Boolean = false,
    @Json(name = "SubmissionDate") val submissionDate: String? = null,
    @Json(name = "SubmittedByOfficer") val submittedByOfficer: String? = null,
    @Json(name = "IsVerified") val isVerified: Boolean = false
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "detectives")
data class DetectiveEntity(
    @PrimaryKey(autoGenerate = true)
    @Json(name = "DetectiveID") val id: Int = 0,
    @Json(name = "FirstName") val firstName: String,
    @Json(name = "LastName") val lastName: String,
    @Json(name = "BadgeNumber") val badgeNumber: String,
    @Json(name = "Rank") val rank: String? = null,
    @Json(name = "Department") val department: String? = null,
    @Json(name = "CurrentWorkload") val currentWorkload: Int = 0
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "court_hearings")
data class CourtHearingEntity(
    @PrimaryKey(autoGenerate = true)
    @Json(name = "HearingID") val id: Int = 0,
    @Json(name = "CaseID") val caseId: Int,
    @Json(name = "CourtName") val courtName: String,
    @Json(name = "JudgeName") val judgeName: String,
    @Json(name = "HearingDate") val hearingDate: String? = null,
    @Json(name = "Verdict") val verdict: String? = null
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "jail_records")
data class JailRecordEntity(
    @PrimaryKey(autoGenerate = true)
    @Json(name = "RecordID") val id: Int = 0,
    @Json(name = "SuspectID") val suspectId: Int,
    @Json(name = "FacilityName") val facilityName: String,
    @Json(name = "CellAssignment") val cellAssignment: String? = null,
    @Json(name = "BookingDate") val bookingDate: String? = null,
    @Json(name = "TransferHistory") val transferHistory: String? = null,
    @Json(name = "ReleaseDate") val releaseDate: String? = null
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true)
    @Json(name = "VehicleID") val id: Int = 0,
    @Json(name = "CaseID") val caseId: Int,
    @Json(name = "PlateNumber") val plateNumber: String,
    @Json(name = "Model") val model: String? = null,
    @Json(name = "IsStolen") val isStolen: Boolean = true
)

// Request payloads
@JsonClass(generateAdapter = true)
data class AssignDetectiveRequest(
    @Json(name = "CaseID") val caseId: Int,
    @Json(name = "CrimeType") val crimeType: String
)

@JsonClass(generateAdapter = true)
data class RiskScoreResponse(
    @Json(name = "RiskScore") val riskScore: Int
)

@JsonClass(generateAdapter = true)
data class SimpleMessageResponse(
    @Json(name = "message") val message: String
)
