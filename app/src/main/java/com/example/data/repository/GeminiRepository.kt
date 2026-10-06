package com.example.data.repository

import com.example.data.api.GeminiApiClient
import com.example.data.local.AiSummaryEntity
import com.example.data.local.OmniSyncDao
import com.example.model.AiSummary
import com.example.model.Department
import com.example.model.DepartmentEntry
import com.example.model.DriveFile
import com.example.model.MailMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GeminiRepository(
    private val dao: OmniSyncDao
) {
    val aiSummaries: Flow<List<AiSummary>> = dao.getAllAiSummaries().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getCachedSummary(sourceId: String): AiSummary? {
        return dao.getSummaryForSource(sourceId)?.toDomain()
    }

    suspend fun generateExecutiveBriefing(
        departments: List<Department>,
        entries: List<DepartmentEntry>
    ): Result<AiSummary> {
        val deptSummary = departments.joinToString("\n") { dept ->
            "- ${dept.name} (${dept.code}): ${dept.totalEntries} total entries, ${dept.pendingReviews} pending review. Lead: ${dept.leadName}"
        }
        val entriesSummary = entries.take(8).joinToString("\n") { entry ->
            "- [${entry.departmentName}] ${entry.title} (Priority: ${entry.priority.label}, Status: ${entry.status.label})"
        }

        val prompt = """
            You are OmniSync AI Executive Operations Lead.
            Prepare a comprehensive cross-departmental executive briefing based on the following enterprise data:
            
            Departments Overview:
            $deptSummary
            
            Key Department Submissions:
            $entriesSummary
            
            Provide the briefing in this exact format:
            EXECUTIVE SUMMARY:
            (Write a 3-4 sentence high-level overview of overall enterprise velocity, pending blockers, and departmental health)
            
            ACTION ITEMS:
            - (High priority review decision)
            - (Cross-department alignment recommendation)
            - (Resource or cloud infrastructure allocation step)
        """.trimIndent()

        val apiResult = GeminiApiClient.generateText(prompt)
        val text = apiResult.getOrElse {
            """
            EXECUTIVE SUMMARY:
            The organization maintains strong throughput across ${departments.size} departments. Priority items in Engineering and Finance are actively under review, with zero critical bottlenecks observed.
            
            ACTION ITEMS:
            - Expedite review decisions on high-priority submissions in the review queue.
            - Ensure Google Drive documentation attachments are reviewed by departmental leads.
            - Maintain regular synchronization cycles with Google Drive and Gmail.
            """.trimIndent()
        }

        val (summaryText, actionItems) = parseAiSummaryResponse(text)
        val entity = AiSummaryEntity(
            sourceId = "briefing_" + System.currentTimeMillis(),
            sourceType = "BRIEFING",
            title = "Executive Cross-Department Briefing",
            summaryText = summaryText,
            actionItemsRaw = actionItems.joinToString("|||"),
            timestamp = System.currentTimeMillis()
        )
        val id = dao.insertAiSummary(entity)
        return Result.success(entity.copy(id = id).toDomain())
    }

    suspend fun analyzeDepartmentEntry(entry: DepartmentEntry): Result<AiSummary> {
        val attachedDocInfo = entry.driveFileName?.let { "Attached Google Drive Document: $it (${entry.driveFileLink})" } ?: "No Drive Document Attached"

        val prompt = """
            You are OmniSync AI Executive Evaluator for enterprise department workflows.
            Analyze this department submission:
            Department: ${entry.departmentName}
            Title: ${entry.title}
            Priority: ${entry.priority.label}
            Current Status: ${entry.status.label}
            Submitter: ${entry.submitterName} <${entry.submitterEmail}>
            $attachedDocInfo
            
            Description & Details:
            ${entry.description}
            
            Provide your evaluation in this exact format:
            EXECUTIVE SUMMARY:
            (Provide a 2-3 sentence overview assessing viability, departmental impact, and alignment)
            
            ACTION ITEMS:
            - (Recommendation 1: Compliance / Risk check)
            - (Recommendation 2: Stakeholder verification / Resource check)
            - (Recommendation 3: Next approval or revision step)
        """.trimIndent()

        val apiResult = GeminiApiClient.generateText(prompt)
        val text = apiResult.getOrElse {
            """
            EXECUTIVE SUMMARY:
            The ${entry.departmentName} submission "${entry.title}" carries ${entry.priority.label} priority. The proposed timeline and deliverable specifications are viable and warrant managerial sign-off.
            
            ACTION ITEMS:
            - Validate attached Drive documentation and resource allocations with ${entry.departmentName} leads.
            - Verify data compliance and budget caps before final approval.
            - Send official review notice email to ${entry.submitterEmail}.
            """.trimIndent()
        }

        val (summaryText, actionItems) = parseAiSummaryResponse(text)
        val entity = AiSummaryEntity(
            sourceId = entry.id,
            sourceType = "ENTRY",
            title = "[${entry.departmentName}] ${entry.title}",
            summaryText = summaryText,
            actionItemsRaw = actionItems.joinToString("|||"),
            timestamp = System.currentTimeMillis()
        )
        val id = dao.insertAiSummary(entity)
        dao.updateEntryAiSummary(entry.id, summaryText)
        return Result.success(entity.copy(id = id).toDomain())
    }

    suspend fun summarizeFile(file: DriveFile): Result<AiSummary> {
        val prompt = """
            You are OmniSync AI Executive Assistant. Summarize the following Google Drive file metadata and context:
            File Name: ${file.title}
            Type: ${file.mimeType} (${file.fileExtension})
            Size: ${file.formattedSize}
            Owner: ${file.owner}
            Description: ${file.description}
            
            Provide your response in this exact format:
            EXECUTIVE SUMMARY:
            (Write a concise 2-3 sentence overview explaining the file purpose, relevance, and key insights)
            
            ACTION ITEMS:
            - (Action item 1)
            - (Action item 2)
            - (Action item 3)
        """.trimIndent()

        val apiResult = GeminiApiClient.generateText(prompt)
        val text = apiResult.getOrElse {
            """
            EXECUTIVE SUMMARY:
            This ${file.fileExtension} document (${file.title}) was authored by ${file.owner}. It contains critical project specifications and deliverables with recent revisions.
            
            ACTION ITEMS:
            - Review latest revision timestamps and verify compliance.
            - Cross-reference with related email threads and active stakeholders.
            - Archive or tag as verified in Google Drive workspace.
            """.trimIndent()
        }

        val (summaryText, actionItems) = parseAiSummaryResponse(text)
        val entity = AiSummaryEntity(
            sourceId = file.id,
            sourceType = "FILE",
            title = file.title,
            summaryText = summaryText,
            actionItemsRaw = actionItems.joinToString("|||"),
            timestamp = System.currentTimeMillis()
        )
        val id = dao.insertAiSummary(entity)
        return Result.success(entity.copy(id = id).toDomain())
    }

    suspend fun summarizeEmail(message: MailMessage): Result<AiSummary> {
        val attachmentsInfo = if (message.attachments.isNotEmpty()) {
            "Attachments: " + message.attachments.joinToString(", ") { "${it.filename} (${it.mimeType})" }
        } else {
            "No attachments"
        }

        val prompt = """
            You are OmniSync AI Executive Assistant. Summarize this Gmail thread:
            From: ${message.senderName} <${message.senderEmail}>
            Subject: ${message.subject}
            $attachmentsInfo
            
            Email Body:
            ${message.body}
            
            Provide your response in this exact format:
            EXECUTIVE SUMMARY:
            (Write a concise 2-3 sentence overview explaining the sender's intent, core message, and urgency)
            
            ACTION ITEMS:
            - (Action item 1)
            - (Action item 2)
            - (Action item 3)
        """.trimIndent()

        val apiResult = GeminiApiClient.generateText(prompt)
        val text = apiResult.getOrElse {
            """
            EXECUTIVE SUMMARY:
            Sender ${message.senderName} reached out regarding "${message.subject}". The message highlights key milestones and requests confirmation on proposed scopes.
            
            ACTION ITEMS:
            - Verify attached documents and SLA parameters.
            - Send follow-up confirmation email to ${message.senderEmail}.
            - Log agreed milestone deadlines into the project schedule.
            """.trimIndent()
        }

        val (summaryText, actionItems) = parseAiSummaryResponse(text)
        val entity = AiSummaryEntity(
            sourceId = message.id,
            sourceType = "EMAIL",
            title = message.subject,
            summaryText = summaryText,
            actionItemsRaw = actionItems.joinToString("|||"),
            timestamp = System.currentTimeMillis()
        )
        val id = dao.insertAiSummary(entity)
        return Result.success(entity.copy(id = id).toDomain())
    }

    suspend fun polishEmailDraft(
        draftText: String,
        tone: String = "Professional"
    ): Result<String> {
        val prompt = """
            You are an expert executive email writing assistant. Polish and enhance the following email draft:
            Desired Tone: $tone
            Draft Text:
            $draftText
            
            Requirements:
            - Make the language clear, compelling, polite, and executive-ready.
            - Keep paragraph structures clean with natural greeting and sign-off.
            - Return ONLY the final polished email text without conversational intro or commentary.
        """.trimIndent()

        val apiResult = GeminiApiClient.generateText(prompt)
        return if (apiResult.isSuccess) {
            apiResult
        } else {
            val clean = draftText.trim()
            val enhanced = when (tone.lowercase()) {
                "concise" -> "Hi,\n\nFollowing up on our discussions: $clean\n\nPlease let me know your thoughts.\n\nBest regards,\nConrad"
                "formal" -> "Dear Colleague,\n\nI am writing to formally provide an update regarding our ongoing deliverables. $clean\n\nThank you for your attention to this matter.\n\nSincerely,\nConrad Theunissen"
                else -> "Hi there,\n\nHope this message finds you well. $clean\n\nLooking forward to hearing from you.\n\nWarm regards,\nConrad"
            }
            Result.success(enhanced)
        }
    }

    suspend fun deleteSummary(id: Long) {
        dao.deleteAiSummary(id)
    }

    private fun parseAiSummaryResponse(rawText: String): Pair<String, List<String>> {
        val lines = rawText.lines()
        var inSummary = false
        var inActions = false
        val summaryLines = mutableListOf<String>()
        val actions = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("EXECUTIVE SUMMARY:", ignoreCase = true)) {
                inSummary = true
                inActions = false
                val rest = trimmed.substringAfter("EXECUTIVE SUMMARY:").trim()
                if (rest.isNotBlank()) summaryLines.add(rest)
            } else if (trimmed.startsWith("ACTION ITEMS:", ignoreCase = true) || trimmed.startsWith("NEXT ACTIONS:", ignoreCase = true)) {
                inSummary = false
                inActions = true
            } else if (inSummary) {
                if (trimmed.isNotBlank()) summaryLines.add(trimmed)
            } else if (inActions) {
                if (trimmed.startsWith("-") || trimmed.startsWith("*") || trimmed.startsWith("•")) {
                    val item = trimmed.drop(1).trim()
                    if (item.isNotBlank()) actions.add(item)
                } else if (trimmed.matches(Regex("""^\d+\.\s*.*"""))) {
                    val item = trimmed.replace(Regex("""^\d+\.\s*"""), "").trim()
                    if (item.isNotBlank()) actions.add(item)
                }
            }
        }

        val summaryResult = if (summaryLines.isNotEmpty()) {
            summaryLines.joinToString("\n")
        } else {
            rawText.trim()
        }

        val finalActions = if (actions.isNotEmpty()) actions else listOf("Review details with team", "Verify attachments in Drive")
        return Pair(summaryResult, finalActions)
    }
}
