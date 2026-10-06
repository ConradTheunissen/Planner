package com.example.model

enum class EntryPriority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    URGENT("Urgent")
}

enum class EntryStatus(val label: String) {
    PENDING("Pending"),
    IN_REVIEW("In Review"),
    APPROVED("Approved"),
    REJECTED("Rejected")
}

enum class SyncStatus(val label: String) {
    SYNCED("Synced with Google"),
    PENDING_SYNC("Pending Sync"),
    SYNCING("Syncing..."),
    FAILED("Sync Error")
}

enum class EntrySortOrder(val label: String) {
    DATE_DESC("Newest"),
    PRIORITY_DESC("Priority"),
    STATUS("Status")
}

data class Department(
    val id: String,
    val name: String,
    val code: String,
    val description: String,
    val leadName: String,
    val leadEmail: String,
    val colorHex: Long,
    val totalEntries: Int = 0,
    val pendingReviews: Int = 0
)

data class DepartmentEntry(
    val id: String,
    val departmentId: String,
    val departmentName: String,
    val title: String,
    val description: String,
    val submitterName: String,
    val submitterEmail: String,
    val priority: EntryPriority,
    val status: EntryStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val reviewNotes: String = "",
    val driveFileId: String? = null,
    val driveFileName: String? = null,
    val driveFileLink: String? = null,
    val aiSummary: String? = null,
    // Sync metadata
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val remoteDriveId: String? = null,
    val remoteMailId: String? = null,
    val syncError: String? = null
)

data class DriveFile(
    val id: String,
    val title: String,
    val mimeType: String,
    val sizeBytes: Long,
    val modifiedTime: Long,
    val webViewLink: String = "",
    val isStarred: Boolean = false,
    val isShared: Boolean = false,
    val owner: String = "Me",
    val description: String = ""
) {
    val fileExtension: String
        get() = when {
            mimeType.contains("pdf") -> "PDF"
            mimeType.contains("word") || mimeType.contains("document") -> "DOC"
            mimeType.contains("sheet") || mimeType.contains("spreadsheet") || mimeType.contains("excel") -> "XLS"
            mimeType.contains("presentation") || mimeType.contains("powerpoint") -> "PPT"
            mimeType.contains("image") -> "IMG"
            mimeType.contains("zip") || mimeType.contains("compressed") -> "ZIP"
            else -> "FILE"
        }

    val formattedSize: String
        get() = when {
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
            else -> String.format("%.1f MB", sizeBytes.toDouble() / (1024 * 1024))
        }
}

data class MailAttachment(
    val id: String,
    val filename: String,
    val mimeType: String,
    val sizeBytes: Long,
    val driveFileId: String? = null
)

data class MailMessage(
    val id: String,
    val threadId: String,
    val senderName: String,
    val senderEmail: String,
    val recipient: String,
    val subject: String,
    val snippet: String,
    val body: String,
    val timestamp: Long,
    val isStarred: Boolean = false,
    val isUnread: Boolean = false,
    val attachments: List<MailAttachment> = emptyList()
)

data class AiSummary(
    val id: Long = 0,
    val sourceId: String,
    val sourceType: String, // "FILE", "EMAIL", or "ENTRY"
    val title: String,
    val summaryText: String,
    val actionItems: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)
