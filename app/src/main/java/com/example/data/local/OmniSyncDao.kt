package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OmniSyncDao {
    // Departments
    @Query("SELECT * FROM departments ORDER BY name ASC")
    fun getAllDepartments(): Flow<List<DepartmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDepartments(departments: List<DepartmentEntity>)

    @Query("SELECT * FROM departments WHERE id = :id LIMIT 1")
    suspend fun getDepartmentById(id: String): DepartmentEntity?

    // Department Entries
    @Query("SELECT * FROM department_entries ORDER BY createdAt DESC")
    fun getAllEntries(): Flow<List<DepartmentEntryEntity>>

    @Query("SELECT * FROM department_entries WHERE departmentId = :deptId ORDER BY createdAt DESC")
    fun getEntriesForDepartment(deptId: String): Flow<List<DepartmentEntryEntity>>

    @Query("SELECT * FROM department_entries WHERE statusStr = 'PENDING' OR statusStr = 'IN_REVIEW' ORDER BY createdAt DESC")
    fun getPendingReviewEntries(): Flow<List<DepartmentEntryEntity>>

    @Query("SELECT * FROM department_entries WHERE syncStatus != 'SYNCED' ORDER BY createdAt DESC")
    fun getUnsyncedEntries(): Flow<List<DepartmentEntryEntity>>

    @Query("SELECT * FROM department_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: String): DepartmentEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DepartmentEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<DepartmentEntryEntity>)

    @Query("UPDATE department_entries SET statusStr = :statusStr, reviewNotes = :reviewNotes, updatedAt = :updatedAt, syncStatus = 'PENDING_SYNC' WHERE id = :id")
    suspend fun updateEntryStatus(id: String, statusStr: String, reviewNotes: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE department_entries SET syncStatus = :syncStatus, lastSyncedAt = :syncedAt, syncError = :error WHERE id = :id")
    suspend fun updateSyncStatus(id: String, syncStatus: String, syncedAt: Long = System.currentTimeMillis(), error: String? = null)

    @Query("UPDATE department_entries SET remoteDriveId = :remoteDriveId, remoteMailId = :remoteMailId, syncStatus = 'SYNCED', lastSyncedAt = :syncedAt, syncError = null WHERE id = :id")
    suspend fun markEntrySynced(id: String, remoteDriveId: String?, remoteMailId: String?, syncedAt: Long = System.currentTimeMillis())

    @Query("UPDATE department_entries SET aiSummary = :summary WHERE id = :id")
    suspend fun updateEntryAiSummary(id: String, summary: String)

    @Query("DELETE FROM department_entries WHERE id = :id")
    suspend fun deleteEntry(id: String)

    // Drive Files
    @Query("SELECT * FROM drive_files ORDER BY modifiedTime DESC")
    fun getAllDriveFiles(): Flow<List<DriveFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriveFiles(files: List<DriveFileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriveFile(file: DriveFileEntity)

    @Query("UPDATE drive_files SET isStarred = :isStarred WHERE id = :id")
    suspend fun updateFileStarred(id: String, isStarred: Boolean)

    @Query("DELETE FROM drive_files WHERE id = :id")
    suspend fun deleteDriveFile(id: String)

    // Mail Messages
    @Query("SELECT * FROM mail_messages ORDER BY timestamp DESC")
    fun getAllMailMessages(): Flow<List<MailMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMailMessages(messages: List<MailMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMailMessage(message: MailMessageEntity)

    @Query("UPDATE mail_messages SET isStarred = :isStarred WHERE id = :id")
    suspend fun updateMailStarred(id: String, isStarred: Boolean)

    @Query("UPDATE mail_messages SET isUnread = 0 WHERE id = :id")
    suspend fun markMailRead(id: String)

    @Query("DELETE FROM mail_messages WHERE id = :id")
    suspend fun deleteMailMessage(id: String)

    // AI Summaries
    @Query("SELECT * FROM ai_summaries ORDER BY timestamp DESC")
    fun getAllAiSummaries(): Flow<List<AiSummaryEntity>>

    @Query("SELECT * FROM ai_summaries WHERE sourceId = :sourceId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getSummaryForSource(sourceId: String): AiSummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiSummary(summary: AiSummaryEntity): Long

    @Query("DELETE FROM ai_summaries WHERE id = :id")
    suspend fun deleteAiSummary(id: Long)
}
