package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.model.AiSummary
import com.example.model.Department
import com.example.model.DepartmentEntry
import com.example.model.DriveFile
import com.example.model.EntryPriority
import com.example.model.EntryStatus
import com.example.model.MailAttachment
import com.example.model.MailMessage
import com.example.model.SyncStatus

@Entity(
    tableName = "departments",
    indices = [
        Index(value = ["code"], unique = true)
    ]
)
data class DepartmentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val code: String,
    val description: String,
    val leadName: String,
    val leadEmail: String,
    val colorHex: Long
) {
    fun toDomain(totalEntries: Int = 0, pendingReviews: Int = 0) = Department(
        id = id,
        name = name,
        code = code,
        description = description,
        leadName = leadName,
        leadEmail = leadEmail,
        colorHex = colorHex,
        totalEntries = totalEntries,
        pendingReviews = pendingReviews
    )
}

/**
 * Room Entity representing DepartmentEntries stored locally with complete metadata
 * and synchronization tracking (syncStatus, remote IDs, and timestamps).
 */
@Entity(
    tableName = "department_entries",
    indices = [
        Index(value = ["departmentId"]),
        Index(value = ["statusStr"]),
        Index(value = ["syncStatus"]),
        Index(value = ["createdAt"])
    ]
)
data class DepartmentEntryEntity(
    @PrimaryKey val id: String,
    val departmentId: String,
    val departmentName: String,
    val title: String,
    val description: String,
    val submitterName: String,
    val submitterEmail: String,
    val priorityStr: String,
    val statusStr: String,
    val createdAt: Long,
    val updatedAt: Long,
    val reviewNotes: String = "",
    val driveFileId: String? = null,
    val driveFileName: String? = null,
    val driveFileLink: String? = null,
    val aiSummary: String? = null,
    // Sync Metadata
    val syncStatus: String = "SYNCED", // SYNCED, PENDING_SYNC, SYNCING, FAILED
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val remoteDriveId: String? = null,
    val remoteMailId: String? = null,
    val syncError: String? = null
) {
    fun toDomain() = DepartmentEntry(
        id = id,
        departmentId = departmentId,
        departmentName = departmentName,
        title = title,
        description = description,
        submitterName = submitterName,
        submitterEmail = submitterEmail,
        priority = try { EntryPriority.valueOf(priorityStr) } catch (e: Exception) { EntryPriority.MEDIUM },
        status = try { EntryStatus.valueOf(statusStr) } catch (e: Exception) { EntryStatus.PENDING },
        createdAt = createdAt,
        updatedAt = updatedAt,
        reviewNotes = reviewNotes,
        driveFileId = driveFileId,
        driveFileName = driveFileName,
        driveFileLink = driveFileLink,
        aiSummary = aiSummary,
        syncStatus = try { SyncStatus.valueOf(syncStatus) } catch (e: Exception) { SyncStatus.SYNCED },
        lastSyncedAt = lastSyncedAt,
        remoteDriveId = remoteDriveId,
        remoteMailId = remoteMailId,
        syncError = syncError
    )

    companion object {
        fun fromDomain(entry: DepartmentEntry) = DepartmentEntryEntity(
            id = entry.id,
            departmentId = entry.departmentId,
            departmentName = entry.departmentName,
            title = entry.title,
            description = entry.description,
            submitterName = entry.submitterName,
            submitterEmail = entry.submitterEmail,
            priorityStr = entry.priority.name,
            statusStr = entry.status.name,
            createdAt = entry.createdAt,
            updatedAt = entry.updatedAt,
            reviewNotes = entry.reviewNotes,
            driveFileId = entry.driveFileId,
            driveFileName = entry.driveFileName,
            driveFileLink = entry.driveFileLink,
            aiSummary = entry.aiSummary,
            syncStatus = entry.syncStatus.name,
            lastSyncedAt = entry.lastSyncedAt,
            remoteDriveId = entry.remoteDriveId,
            remoteMailId = entry.remoteMailId,
            syncError = entry.syncError
        )
    }
}

@Entity(
    tableName = "drive_files",
    indices = [
        Index(value = ["modifiedTime"]),
        Index(value = ["isStarred"])
    ]
)
data class DriveFileEntity(
    @PrimaryKey val id: String,
    val title: String,
    val mimeType: String,
    val sizeBytes: Long,
    val modifiedTime: Long,
    val webViewLink: String,
    val isStarred: Boolean,
    val isShared: Boolean,
    val owner: String,
    val description: String
) {
    fun toDomain() = DriveFile(
        id = id,
        title = title,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        modifiedTime = modifiedTime,
        webViewLink = webViewLink,
        isStarred = isStarred,
        isShared = isShared,
        owner = owner,
        description = description
    )
}

@Entity(
    tableName = "mail_messages",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["isUnread"])
    ]
)
data class MailMessageEntity(
    @PrimaryKey val id: String,
    val threadId: String,
    val senderName: String,
    val senderEmail: String,
    val recipient: String,
    val subject: String,
    val snippet: String,
    val body: String,
    val timestamp: Long,
    val isStarred: Boolean,
    val isUnread: Boolean,
    val attachmentsJson: String
) {
    fun toDomain(): MailMessage {
        val attachments = if (attachmentsJson.isBlank()) {
            emptyList()
        } else {
            attachmentsJson.split(";").mapNotNull { part ->
                val tokens = part.split("|")
                if (tokens.size >= 4) {
                    MailAttachment(
                        id = tokens[0],
                        filename = tokens[1],
                        mimeType = tokens[2],
                        sizeBytes = tokens[3].toLongOrNull() ?: 0L,
                        driveFileId = tokens.getOrNull(4)
                    )
                } else null
            }
        }
        return MailMessage(
            id = id,
            threadId = threadId,
            senderName = senderName,
            senderEmail = senderEmail,
            recipient = recipient,
            subject = subject,
            snippet = snippet,
            body = body,
            timestamp = timestamp,
            isStarred = isStarred,
            isUnread = isUnread,
            attachments = attachments
        )
    }
}

@Entity(
    tableName = "ai_summaries",
    indices = [
        Index(value = ["sourceId"]),
        Index(value = ["timestamp"])
    ]
)
data class AiSummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: String,
    val sourceType: String,
    val title: String,
    val summaryText: String,
    val actionItemsRaw: String,
    val timestamp: Long
) {
    fun toDomain() = AiSummary(
        id = id,
        sourceId = sourceId,
        sourceType = sourceType,
        title = title,
        summaryText = summaryText,
        actionItems = if (actionItemsRaw.isBlank()) emptyList() else actionItemsRaw.split("|||"),
        timestamp = timestamp
    )
}

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString("|||")

    @TypeConverter
    fun toStringList(value: String): List<String> = if (value.isBlank()) emptyList() else value.split("|||")
}
