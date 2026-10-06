package com.example.data.repository

import android.content.Context
import android.util.Base64
import com.example.data.api.GmailSendMessageRequest
import com.example.data.api.GoogleDriveCreateFileRequest
import com.example.data.api.WorkspaceApiClient
import com.example.data.local.DepartmentEntity
import com.example.data.local.DepartmentEntryEntity
import com.example.data.local.DriveFileEntity
import com.example.data.local.MailMessageEntity
import com.example.data.local.OmniSyncDao
import com.example.model.Department
import com.example.model.DepartmentEntry
import com.example.model.DriveFile
import com.example.model.EntryPriority
import com.example.model.EntryStatus
import com.example.model.MailAttachment
import com.example.model.MailMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class WorkspaceRepository(
    private val dao: OmniSyncDao,
    private val context: Context
) {
    // Observable streams
    val driveFiles: Flow<List<DriveFile>> = dao.getAllDriveFiles().map { list ->
        list.map { it.toDomain() }
    }

    val mailMessages: Flow<List<MailMessage>> = dao.getAllMailMessages().map { list ->
        list.map { it.toDomain() }
    }

    val allEntries: Flow<List<DepartmentEntry>> = dao.getAllEntries().map { list ->
        list.map { it.toDomain() }
    }

    val departments: Flow<List<Department>> = combine(
        dao.getAllDepartments(),
        dao.getAllEntries()
    ) { deptEntities, entryEntities ->
        deptEntities.map { dept ->
            val entriesForDept = entryEntities.filter { it.departmentId == dept.id }
            val pendingCount = entriesForDept.count { it.statusStr == "PENDING" || it.statusStr == "IN_REVIEW" }
            dept.toDomain(
                totalEntries = entriesForDept.size,
                pendingReviews = pendingCount
            )
        }
    }

    suspend fun initializeStarterDataIfEmpty() {
        val now = System.currentTimeMillis()
        val oneHour = 3600 * 1000L
        val oneDay = 24 * oneHour

        // 1. Core Enterprise Departments
        val starterDepts = listOf(
            DepartmentEntity(
                id = "dept_eng",
                name = "Engineering",
                code = "ENG",
                description = "Architecture specs, platform engineering, and technical roadmap execution.",
                leadName = "Marcus Vance",
                leadEmail = "marcus.vance@techlead.org",
                colorHex = 0xFF2563EB
            ),
            DepartmentEntity(
                id = "dept_fin",
                name = "Finance",
                code = "FIN",
                description = "Quarterly forecasting, cloud compute budget, and vendor allocation.",
                leadName = "Elena Rostova",
                leadEmail = "elena.r@ventures.io",
                colorHex = 0xFF059669
            ),
            DepartmentEntity(
                id = "dept_ops",
                name = "Operations",
                code = "OPS",
                description = "Infrastructure reliability, SLA enforcement, and operational readiness.",
                leadName = "David Zhang",
                leadEmail = "david.zhang@ops.org",
                colorHex = 0xFFD97706
            ),
            DepartmentEntity(
                id = "dept_leg",
                name = "Legal",
                code = "LEG",
                description = "Master service agreements, compliance audits, and data governance.",
                leadName = "Sophia Martinez",
                leadEmail = "sophia.m@legalcorp.com",
                colorHex = 0xFF7C3AED
            ),
            DepartmentEntity(
                id = "dept_hr",
                name = "Human Resources",
                code = "HR",
                description = "Workforce planning, talent acquisition, and company policy review.",
                leadName = "Amina Yusuf",
                leadEmail = "amina.y@talentgroup.io",
                colorHex = 0xFFE11D48
            )
        )

        // 2. Drive Files
        val starterFiles = listOf(
            DriveFileEntity(
                id = "doc_proj_spec",
                title = "Q4 Product Strategy & Roadmap.docx",
                mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                sizeBytes = 2_450_000L,
                modifiedTime = now - (2 * oneHour),
                webViewLink = "https://docs.google.com/document/d/sample1",
                isStarred = true,
                isShared = true,
                owner = "Conrad Theunissen",
                description = "Comprehensive engineering goals, milestone delivery dates, and API SLAs."
            ),
            DriveFileEntity(
                id = "sheet_budget_2026",
                title = "Annual Financial Forecast 2026-2027.xlsx",
                mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                sizeBytes = 1_820_000L,
                modifiedTime = now - (5 * oneHour),
                webViewLink = "https://docs.google.com/spreadsheets/d/sample2",
                isStarred = true,
                isShared = false,
                owner = "Conrad Theunissen",
                description = "Cloud server cost breakdown, API rate limits modeling, and headcount allocation."
            ),
            DriveFileEntity(
                id = "pdf_vendor_agreement",
                title = "Master Services Agreement - CloudVendor.pdf",
                mimeType = "application/pdf",
                sizeBytes = 4_120_000L,
                modifiedTime = now - oneDay,
                webViewLink = "https://drive.google.com/file/d/sample3",
                isStarred = false,
                isShared = true,
                owner = "Legal Team",
                description = "Signed terms, 99.99% uptime guarantee, liability coverage, and security riders."
            )
        )

        // 3. Department Entries (from uploaded app workflow)
        val starterEntries = listOf(
            DepartmentEntryEntity(
                id = "entry_001",
                departmentId = "dept_eng",
                departmentName = "Engineering",
                title = "Q4 Microservices Architecture & Migration Plan",
                description = "Proposal to migrate legacy sync jobs to serverless event-driven architecture with OAuth token caching.",
                submitterName = "Conrad Theunissen",
                submitterEmail = "conradtheunissen@gmail.com",
                priorityStr = EntryPriority.HIGH.name,
                statusStr = EntryStatus.IN_REVIEW.name,
                createdAt = now - (3 * oneHour),
                updatedAt = now - (1 * oneHour),
                reviewNotes = "Initial review in progress. Benchmarking latency on Google Cloud Run.",
                driveFileId = "doc_proj_spec",
                driveFileName = "Q4 Product Strategy & Roadmap.docx",
                driveFileLink = "https://docs.google.com/document/d/sample1",
                aiSummary = "Architecture proposal emphasizes zero-downtime transition and token security. Risk factor: potential cold-start overhead."
            ),
            DepartmentEntryEntity(
                id = "entry_002",
                departmentId = "dept_fin",
                departmentName = "Finance",
                title = "2026 Compute Infrastructure & AI Quota Budget",
                description = "Requested expansion for Google Workspace API calls and Gemini 3.5 model inference budget.",
                submitterName = "Marcus Vance",
                submitterEmail = "marcus.vance@techlead.org",
                priorityStr = EntryPriority.URGENT.name,
                statusStr = EntryStatus.PENDING.name,
                createdAt = now - (6 * oneHour),
                updatedAt = now - (6 * oneHour),
                reviewNotes = "",
                driveFileId = "sheet_budget_2026",
                driveFileName = "Annual Financial Forecast 2026-2027.xlsx",
                driveFileLink = "https://docs.google.com/spreadsheets/d/sample2",
                aiSummary = "Projected 18% savings compared to legacy dedicated hosting with dynamic scaling."
            ),
            DepartmentEntryEntity(
                id = "entry_003",
                departmentId = "dept_leg",
                departmentName = "Legal",
                title = "Cloud Vendor Security Addendum & DPA Review",
                description = "Data protection agreement renewal incorporating EU GDPR and California privacy clauses.",
                submitterName = "Sophia Martinez",
                submitterEmail = "sophia.m@legalcorp.com",
                priorityStr = EntryPriority.MEDIUM.name,
                statusStr = EntryStatus.APPROVED.name,
                createdAt = now - (2 * oneDay),
                updatedAt = now - (4 * oneHour),
                reviewNotes = "Approved by General Counsel. Signed PDF deposited in Google Drive legal vault.",
                driveFileId = "pdf_vendor_agreement",
                driveFileName = "Master Services Agreement - CloudVendor.pdf",
                driveFileLink = "https://drive.google.com/file/d/sample3",
                aiSummary = "Standard terms verified; indemnification cap aligned with board guidelines."
            ),
            DepartmentEntryEntity(
                id = "entry_004",
                departmentId = "dept_ops",
                departmentName = "Operations",
                title = "Disaster Recovery Failover Drill Protocol",
                description = "Bi-annual cross-region failover procedure across Europe and US availability zones.",
                submitterName = "David Zhang",
                submitterEmail = "david.zhang@ops.org",
                priorityStr = EntryPriority.HIGH.name,
                statusStr = EntryStatus.PENDING.name,
                createdAt = now - oneDay,
                updatedAt = now - oneDay,
                reviewNotes = "",
                driveFileId = null,
                driveFileName = null,
                driveFileLink = null,
                aiSummary = null
            )
        )

        // 4. Starter Emails
        val starterEmails = listOf(
            MailMessageEntity(
                id = "msg_001",
                threadId = "th_001",
                senderName = "Marcus Vance",
                senderEmail = "marcus.vance@techlead.org",
                recipient = "conradtheunissen@gmail.com",
                subject = "Review Request: Q4 Microservices Architecture & Migration Plan",
                snippet = "Hi Conrad, please review the latest engineering proposal attached from Google Drive before the Thursday sprint planning...",
                body = "Hi Conrad,\n\nI have reviewed the proposal and linked the latest roadmap document from Drive. Please examine the failover SLA metrics before our sync.\n\nBest,\nMarcus Vance",
                timestamp = now - (2 * oneHour),
                isStarred = true,
                isUnread = true,
                attachmentsJson = "att_01|Q4 Product Strategy & Roadmap.docx|application/vnd.openxmlformats-officedocument.wordprocessingml.document|2450000|doc_proj_spec"
            ),
            MailMessageEntity(
                id = "msg_002",
                threadId = "th_002",
                senderName = "Elena Rostova",
                senderEmail = "elena.r@ventures.io",
                recipient = "conradtheunissen@gmail.com",
                subject = "Pending Approval: 2026 Compute Infrastructure Budget",
                snippet = "Hello Conrad, the budget entry is awaiting review. Can you confirm the API quotas?",
                body = "Hello Conrad,\n\nThe spreadsheet has been attached from Drive for your review. Let us know if the unit cost estimates meet your department targets.\n\nWarm regards,\nElena",
                timestamp = now - (5 * oneHour),
                isStarred = false,
                isUnread = true,
                attachmentsJson = "att_02|Annual Financial Forecast 2026-2027.xlsx|application/vnd.openxmlformats-officedocument.spreadsheetml.sheet|1820000|sheet_budget_2026"
            )
        )

        dao.insertDepartments(starterDepts)
        dao.insertDriveFiles(starterFiles)
        dao.insertEntries(starterEntries)
        dao.insertMailMessages(starterEmails)
    }

    suspend fun createDepartmentEntry(
        departmentId: String,
        departmentName: String,
        title: String,
        description: String,
        priority: EntryPriority,
        driveFile: DriveFile? = null,
        sendGmailNotice: Boolean = true,
        authToken: String? = null
    ): Result<DepartmentEntry> {
        val entryId = "entry_" + UUID.randomUUID().toString().take(8)
        val now = System.currentTimeMillis()

        val entity = DepartmentEntryEntity(
            id = entryId,
            departmentId = departmentId,
            departmentName = departmentName,
            title = title,
            description = description,
            submitterName = "Conrad Theunissen",
            submitterEmail = "conradtheunissen@gmail.com",
            priorityStr = priority.name,
            statusStr = EntryStatus.PENDING.name,
            createdAt = now,
            updatedAt = now,
            reviewNotes = "",
            driveFileId = driveFile?.id,
            driveFileName = driveFile?.title,
            driveFileLink = driveFile?.webViewLink,
            aiSummary = null
        )

        dao.insertEntry(entity)

        // If requested, dispatch notification email via Gmail
        if (sendGmailNotice) {
            val emailSubject = "[$departmentName Entry Submitted] $title"
            val fileAttachmentText = driveFile?.let { "\nAttached Drive Document: ${it.title} (${it.webViewLink})" } ?: ""
            val emailBody = "New department entry has been submitted for review:\n\n" +
                    "Department: $departmentName\n" +
                    "Priority: ${priority.label}\n" +
                    "Title: $title\n" +
                    "Summary: $description\n" +
                    fileAttachmentText + "\n\n" +
                    "Submitted by: Conrad Theunissen (conradtheunissen@gmail.com)"

            sendEmail(
                recipient = "conradtheunissen@gmail.com",
                subject = emailSubject,
                body = emailBody,
                attachedDriveFiles = driveFile?.let { listOf(it) } ?: emptyList(),
                authToken = authToken
            )
        }

        return Result.success(entity.toDomain())
    }

    suspend fun updateEntryReview(
        entryId: String,
        newStatus: EntryStatus,
        reviewNotes: String,
        sendNoticeEmail: Boolean = true,
        submitterEmail: String = "conradtheunissen@gmail.com",
        entryTitle: String = "Entry",
        authToken: String? = null
    ) {
        dao.updateEntryStatus(entryId, newStatus.name, reviewNotes)

        if (sendNoticeEmail) {
            val statusLabel = newStatus.label.uppercase()
            val subject = "[$statusLabel] Department Review Decision: $entryTitle"
            val body = "The following department submission has been reviewed:\n\n" +
                    "Entry: $entryTitle\n" +
                    "Decision: ${newStatus.label}\n" +
                    "Reviewer Feedback: ${if (reviewNotes.isNotBlank()) reviewNotes else "No additional notes."}\n\n" +
                    "Reviewed by: Conrad Theunissen\n" +
                    "Notification sent via OmniSync Gmail Service"

            sendEmail(
                recipient = submitterEmail,
                subject = subject,
                body = body,
                attachedDriveFiles = emptyList(),
                authToken = authToken
            )
        }
    }

    suspend fun updateEntryAiSummary(entryId: String, summaryText: String) {
        dao.updateEntryAiSummary(entryId, summaryText)
    }

    suspend fun deleteEntry(entryId: String) {
        dao.deleteEntry(entryId)
    }

    // Drive & Mail methods
    suspend fun toggleFileStarred(fileId: String, currentStarred: Boolean) {
        dao.updateFileStarred(fileId, !currentStarred)
    }

    suspend fun toggleMailStarred(mailId: String, currentStarred: Boolean) {
        dao.updateMailStarred(mailId, !currentStarred)
    }

    suspend fun markMailAsRead(mailId: String) {
        dao.markMailRead(mailId)
    }

    suspend fun createDriveFile(
        title: String,
        mimeType: String,
        description: String,
        authToken: String? = null
    ): Result<DriveFile> {
        val newId = "file_" + UUID.randomUUID().toString().take(8)
        val entity = DriveFileEntity(
            id = newId,
            title = title,
            mimeType = mimeType,
            sizeBytes = (5000..50000).random().toLong(),
            modifiedTime = System.currentTimeMillis(),
            webViewLink = "https://drive.google.com/file/d/$newId",
            isStarred = false,
            isShared = false,
            owner = "Conrad Theunissen",
            description = description
        )

        if (!authToken.isNullOrBlank()) {
            try {
                WorkspaceApiClient.driveService.createFile(
                    authHeader = "Bearer $authToken",
                    request = GoogleDriveCreateFileRequest(
                        name = title,
                        mimeType = mimeType,
                        description = description
                    )
                )
            } catch (e: Exception) {
                // Keep local cache resilience
            }
        }

        dao.insertDriveFile(entity)
        return Result.success(entity.toDomain())
    }

    suspend fun sendEmail(
        recipient: String,
        subject: String,
        body: String,
        attachedDriveFiles: List<DriveFile>,
        authToken: String? = null
    ): Result<MailMessage> {
        val newMsgId = "msg_" + UUID.randomUUID().toString().take(8)
        val threadId = "th_" + UUID.randomUUID().toString().take(8)

        val attachmentsFormatted = attachedDriveFiles.joinToString(";") { file ->
            "att_${UUID.randomUUID().toString().take(6)}|${file.title}|${file.mimeType}|${file.sizeBytes}|${file.id}"
        }

        val entity = MailMessageEntity(
            id = newMsgId,
            threadId = threadId,
            senderName = "Conrad Theunissen",
            senderEmail = "conradtheunissen@gmail.com",
            recipient = recipient,
            subject = subject,
            snippet = body.take(90).replace("\n", " "),
            body = body,
            timestamp = System.currentTimeMillis(),
            isStarred = false,
            isUnread = false,
            attachmentsJson = attachmentsFormatted
        )

        if (!authToken.isNullOrBlank()) {
            try {
                val rawEmail = "To: $recipient\r\nSubject: $subject\r\n\r\n$body"
                val encoded = Base64.encodeToString(rawEmail.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
                WorkspaceApiClient.gmailService.sendMessage(
                    authHeader = "Bearer $authToken",
                    request = GmailSendMessageRequest(raw = encoded)
                )
            } catch (e: Exception) {
                // Continue with local save
            }
        }

        dao.insertMailMessage(entity)
        return Result.success(entity.toDomain())
    }

    suspend fun deleteFile(fileId: String) {
        dao.deleteDriveFile(fileId)
    }

    suspend fun deleteMail(mailId: String) {
        dao.deleteMailMessage(mailId)
    }
}
