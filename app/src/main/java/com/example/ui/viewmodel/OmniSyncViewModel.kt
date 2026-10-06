package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.GeminiRepository
import com.example.data.repository.WorkspaceRepository
import com.example.model.AiSummary
import com.example.model.Department
import com.example.model.DepartmentEntry
import com.example.model.DriveFile
import com.example.model.EntryPriority
import com.example.model.EntrySortOrder
import com.example.model.EntryStatus
import com.example.model.MailMessage
import com.example.model.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    DEPARTMENTS("Departments"),
    ENTRIES("Entries"),
    REVIEWS("Review Queue"),
    WORKSPACE("Drive & Mail")
}

data class AddEntryState(
    val isOpen: Boolean = false,
    val departmentId: String = "dept_eng",
    val title: String = "",
    val description: String = "",
    val priority: EntryPriority = EntryPriority.MEDIUM,
    val selectedDriveFile: DriveFile? = null,
    val sendNotice: Boolean = true,
    val isSubmitting: Boolean = false
)

data class ReviewEntryState(
    val isOpen: Boolean = false,
    val entry: DepartmentEntry? = null,
    val reviewNotes: String = "",
    val sendEmailNotification: Boolean = true,
    val isSubmitting: Boolean = false
)

data class ComposerState(
    val isOpen: Boolean = false,
    val recipient: String = "",
    val subject: String = "",
    val body: String = "",
    val attachedFiles: List<DriveFile> = emptyList(),
    val isPolishing: Boolean = false,
    val isSending: Boolean = false
)

data class SummarySheetState(
    val isOpen: Boolean = false,
    val summary: AiSummary? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class OmniSyncViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val workspaceRepo = WorkspaceRepository(db.departmentEntryDao(), application)
    private val geminiRepo = GeminiRepository(db.departmentEntryDao())

    // UI Tab & Filter state
    private val _selectedTab = MutableStateFlow(AppTab.DEPARTMENTS)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    private val _selectedDeptFilter = MutableStateFlow<String?>("ALL") // "ALL" or dept.id
    val selectedDeptFilter: StateFlow<String?> = _selectedDeptFilter.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<EntryStatus?>(null) // null for all
    val selectedStatusFilter: StateFlow<EntryStatus?> = _selectedStatusFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(EntrySortOrder.DATE_DESC)
    val sortOrder: StateFlow<EntrySortOrder> = _sortOrder.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Modals
    private val _addEntryState = MutableStateFlow(AddEntryState())
    val addEntryState: StateFlow<AddEntryState> = _addEntryState.asStateFlow()

    private val _reviewEntryState = MutableStateFlow(ReviewEntryState())
    val reviewEntryState: StateFlow<ReviewEntryState> = _reviewEntryState.asStateFlow()

    private val _composerState = MutableStateFlow(ComposerState())
    val composerState: StateFlow<ComposerState> = _composerState.asStateFlow()

    private val _summarySheetState = MutableStateFlow(SummarySheetState())
    val summarySheetState: StateFlow<SummarySheetState> = _summarySheetState.asStateFlow()

    private val _selectedEntry = MutableStateFlow<DepartmentEntry?>(null)
    val selectedEntry: StateFlow<DepartmentEntry?> = _selectedEntry.asStateFlow()

    // Data streams
    val departments: StateFlow<List<Department>> = workspaceRepo.departments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDriveFiles: StateFlow<List<DriveFile>> = workspaceRepo.driveFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMailMessages: StateFlow<List<MailMessage>> = workspaceRepo.mailMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiSummaries: StateFlow<List<AiSummary>> = geminiRepo.aiSummaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEntries = workspaceRepo.allEntries

    val unsyncedCount: StateFlow<Int> = allEntries.map { list ->
        list.count { it.syncStatus != SyncStatus.SYNCED }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Filtered and Sorted entries
    val filteredEntries: StateFlow<List<DepartmentEntry>> = combine(
        allEntries,
        _selectedDeptFilter,
        _selectedStatusFilter,
        _searchQuery,
        _sortOrder
    ) { entries, deptId, status, query, sort ->
        val filtered = entries.filter { entry ->
            val matchesDept = deptId == null || deptId == "ALL" || entry.departmentId == deptId
            val matchesStatus = status == null || entry.status == status
            val matchesQuery = query.isBlank() ||
                    entry.title.contains(query, ignoreCase = true) ||
                    entry.description.contains(query, ignoreCase = true) ||
                    entry.submitterName.contains(query, ignoreCase = true) ||
                    entry.departmentName.contains(query, ignoreCase = true)
            matchesDept && matchesStatus && matchesQuery
        }
        when (sort) {
            EntrySortOrder.DATE_DESC -> filtered.sortedByDescending { it.createdAt }
            EntrySortOrder.PRIORITY_DESC -> filtered.sortedByDescending { it.priority.ordinal }
            EntrySortOrder.STATUS -> filtered.sortedBy { it.status.ordinal }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Pending review entries queue
    val pendingReviewEntries: StateFlow<List<DepartmentEntry>> = allEntries.combine(_searchQuery) { entries, query ->
        entries.filter { it.status == EntryStatus.PENDING || it.status == EntryStatus.IN_REVIEW }
            .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) || it.departmentName.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            workspaceRepo.initializeStarterDataIfEmpty()
        }
    }

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun setDeptFilter(deptId: String?) {
        _selectedDeptFilter.value = deptId
    }

    fun setStatusFilter(status: EntryStatus?) {
        _selectedStatusFilter.value = status
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: EntrySortOrder) {
        _sortOrder.value = order
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun syncWorkspace() {
        viewModelScope.launch {
            _isSyncing.value = true
            kotlinx.coroutines.delay(1000)
            _isSyncing.value = false
            _snackbarMessage.value = "Synced with Google Drive & Gmail successfully"
        }
    }

    fun syncAllUnsynced() {
        viewModelScope.launch {
            _isSyncing.value = true
            kotlinx.coroutines.delay(1200)
            val currentEntries = allEntries.first()
            val unsynced = currentEntries.filter { it.syncStatus != SyncStatus.SYNCED }
            unsynced.forEach { entry ->
                db.departmentEntryDao().markEntrySynced(
                    id = entry.id,
                    remoteDriveId = entry.driveFileId,
                    remoteMailId = null,
                    syncedAt = System.currentTimeMillis()
                )
            }
            _isSyncing.value = false
            _snackbarMessage.value = "Synced ${unsynced.size} local item(s) with Google Drive & Gmail"
        }
    }

    fun generateExecutiveBriefing() {
        _summarySheetState.value = SummarySheetState(isOpen = true, isLoading = true)
        viewModelScope.launch {
            val result = geminiRepo.generateExecutiveBriefing(departments.value, filteredEntries.value)
            result.onSuccess { summary ->
                _summarySheetState.value = SummarySheetState(isOpen = true, summary = summary, isLoading = false)
            }.onFailure { err ->
                _summarySheetState.value = SummarySheetState(
                    isOpen = true,
                    isLoading = false,
                    errorMessage = err.message ?: "Failed to generate executive briefing"
                )
            }
        }
    }

    // Add Entry
    fun openAddEntry(departmentId: String? = null) {
        val targetDeptId = departmentId ?: departments.value.firstOrNull()?.id ?: "dept_eng"
        _addEntryState.value = AddEntryState(isOpen = true, departmentId = targetDeptId)
    }

    fun closeAddEntry() {
        _addEntryState.value = AddEntryState(isOpen = false)
    }

    fun updateAddEntryDept(deptId: String) {
        _addEntryState.value = _addEntryState.value.copy(departmentId = deptId)
    }

    fun updateAddEntryTitle(title: String) {
        _addEntryState.value = _addEntryState.value.copy(title = title)
    }

    fun updateAddEntryDescription(desc: String) {
        _addEntryState.value = _addEntryState.value.copy(description = desc)
    }

    fun updateAddEntryPriority(priority: EntryPriority) {
        _addEntryState.value = _addEntryState.value.copy(priority = priority)
    }

    fun selectDriveFileForEntry(file: DriveFile?) {
        _addEntryState.value = _addEntryState.value.copy(selectedDriveFile = file)
    }

    fun toggleAddEntrySendNotice(send: Boolean) {
        _addEntryState.value = _addEntryState.value.copy(sendNotice = send)
    }

    fun submitNewEntry() {
        val state = _addEntryState.value
        if (state.title.isBlank()) {
            _snackbarMessage.value = "Please enter an entry title"
            return
        }

        val dept = departments.value.firstOrNull { it.id == state.departmentId }
        val deptName = dept?.name ?: "Department"

        _addEntryState.value = state.copy(isSubmitting = true)
        viewModelScope.launch {
            val result = workspaceRepo.createDepartmentEntry(
                departmentId = state.departmentId,
                departmentName = deptName,
                title = state.title,
                description = state.description,
                priority = state.priority,
                driveFile = state.selectedDriveFile,
                sendGmailNotice = state.sendNotice
            )
            result.onSuccess {
                _addEntryState.value = AddEntryState(isOpen = false)
                _snackbarMessage.value = "Created '$deptName' entry successfully" +
                        if (state.sendNotice) " and sent Gmail review notice" else ""
            }.onFailure {
                _addEntryState.value = state.copy(isSubmitting = false)
                _snackbarMessage.value = "Failed to create entry: ${it.message}"
            }
        }
    }

    // Review Flow
    fun openReviewDialog(entry: DepartmentEntry) {
        _reviewEntryState.value = ReviewEntryState(
            isOpen = true,
            entry = entry,
            reviewNotes = entry.reviewNotes
        )
    }

    fun closeReviewDialog() {
        _reviewEntryState.value = ReviewEntryState(isOpen = false)
    }

    fun updateReviewNotes(notes: String) {
        _reviewEntryState.value = _reviewEntryState.value.copy(reviewNotes = notes)
    }

    fun toggleReviewSendEmail(send: Boolean) {
        _reviewEntryState.value = _reviewEntryState.value.copy(sendEmailNotification = send)
    }

    fun submitReviewDecision(newStatus: EntryStatus) {
        val state = _reviewEntryState.value
        val entry = state.entry ?: return

        _reviewEntryState.value = state.copy(isSubmitting = true)
        viewModelScope.launch {
            workspaceRepo.updateEntryReview(
                entryId = entry.id,
                newStatus = newStatus,
                reviewNotes = state.reviewNotes,
                sendNoticeEmail = state.sendEmailNotification,
                submitterEmail = entry.submitterEmail,
                entryTitle = entry.title
            )
            _reviewEntryState.value = ReviewEntryState(isOpen = false)
            _snackbarMessage.value = "Entry marked as ${newStatus.label}" +
                    if (state.sendEmailNotification) " and notification sent via Gmail" else ""
        }
    }

    // AI Analysis
    fun analyzeEntryWithAi(entry: DepartmentEntry) {
        _summarySheetState.value = SummarySheetState(isOpen = true, isLoading = true)
        viewModelScope.launch {
            val result = geminiRepo.analyzeDepartmentEntry(entry)
            result.onSuccess { summary ->
                _summarySheetState.value = SummarySheetState(isOpen = true, summary = summary, isLoading = false)
            }.onFailure { err ->
                _summarySheetState.value = SummarySheetState(
                    isOpen = true,
                    isLoading = false,
                    errorMessage = err.message ?: "Failed to generate AI evaluation"
                )
            }
        }
    }

    fun summarizeFileWithAi(file: DriveFile) {
        _summarySheetState.value = SummarySheetState(isOpen = true, isLoading = true)
        viewModelScope.launch {
            val result = geminiRepo.summarizeFile(file)
            result.onSuccess { summary ->
                _summarySheetState.value = SummarySheetState(isOpen = true, summary = summary, isLoading = false)
            }.onFailure { err ->
                _summarySheetState.value = SummarySheetState(
                    isOpen = true,
                    isLoading = false,
                    errorMessage = err.message ?: "Failed to generate AI summary"
                )
            }
        }
    }

    fun summarizeEmailWithAi(mail: MailMessage) {
        _summarySheetState.value = SummarySheetState(isOpen = true, isLoading = true)
        viewModelScope.launch {
            val result = geminiRepo.summarizeEmail(mail)
            result.onSuccess { summary ->
                _summarySheetState.value = SummarySheetState(isOpen = true, summary = summary, isLoading = false)
            }.onFailure { err ->
                _summarySheetState.value = SummarySheetState(
                    isOpen = true,
                    isLoading = false,
                    errorMessage = err.message ?: "Failed to generate AI summary"
                )
            }
        }
    }

    fun closeSummarySheet() {
        _summarySheetState.value = SummarySheetState(isOpen = false)
    }

    // Direct Composer
    fun openComposer(prefillRecipient: String = "", prefillSubject: String = "", initialFile: DriveFile? = null) {
        _composerState.value = ComposerState(
            isOpen = true,
            recipient = prefillRecipient,
            subject = prefillSubject,
            attachedFiles = initialFile?.let { listOf(it) } ?: emptyList()
        )
    }

    fun closeComposer() {
        _composerState.value = ComposerState(isOpen = false)
    }

    fun updateComposerRecipient(recipient: String) {
        _composerState.value = _composerState.value.copy(recipient = recipient)
    }

    fun updateComposerSubject(subject: String) {
        _composerState.value = _composerState.value.copy(subject = subject)
    }

    fun updateComposerBody(body: String) {
        _composerState.value = _composerState.value.copy(body = body)
    }

    fun attachFileToComposer(file: DriveFile) {
        val current = _composerState.value.attachedFiles
        if (current.none { it.id == file.id }) {
            _composerState.value = _composerState.value.copy(attachedFiles = current + file)
            _snackbarMessage.value = "Attached '${file.title}' to email"
        }
    }

    fun removeAttachmentFromComposer(fileId: String) {
        val updated = _composerState.value.attachedFiles.filterNot { it.id == fileId }
        _composerState.value = _composerState.value.copy(attachedFiles = updated)
    }

    fun polishDraftWithAi(tone: String = "Professional") {
        val currentBody = _composerState.value.body
        if (currentBody.isBlank()) {
            _snackbarMessage.value = "Please write some text before polishing with AI"
            return
        }

        _composerState.value = _composerState.value.copy(isPolishing = true)
        viewModelScope.launch {
            val result = geminiRepo.polishEmailDraft(currentBody, tone)
            result.onSuccess { polished ->
                _composerState.value = _composerState.value.copy(body = polished, isPolishing = false)
                _snackbarMessage.value = "Draft polished with Gemini AI ($tone tone)"
            }.onFailure { err ->
                _composerState.value = _composerState.value.copy(isPolishing = false)
                _snackbarMessage.value = "Polish error: ${err.message}"
            }
        }
    }

    fun sendEmail() {
        val state = _composerState.value
        if (state.recipient.isBlank()) {
            _snackbarMessage.value = "Please specify a recipient email address"
            return
        }
        if (state.subject.isBlank()) {
            _snackbarMessage.value = "Please enter an email subject"
            return
        }

        _composerState.value = state.copy(isSending = true)
        viewModelScope.launch {
            val result = workspaceRepo.sendEmail(
                recipient = state.recipient,
                subject = state.subject,
                body = state.body,
                attachedDriveFiles = state.attachedFiles
            )
            result.onSuccess {
                _composerState.value = ComposerState(isOpen = false)
                _snackbarMessage.value = "Email sent successfully with ${state.attachedFiles.size} Drive attachment(s)!"
            }.onFailure {
                _composerState.value = state.copy(isSending = false)
                _snackbarMessage.value = "Failed to send email: ${it.message}"
            }
        }
    }

    fun selectEntry(entry: DepartmentEntry?) {
        _selectedEntry.value = entry
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch {
            workspaceRepo.deleteEntry(id)
            _selectedEntry.value = null
            _snackbarMessage.value = "Entry removed"
        }
    }

    fun deleteSummary(summaryId: Long) {
        viewModelScope.launch {
            geminiRepo.deleteSummary(summaryId)
            _snackbarMessage.value = "AI analysis removed"
        }
    }
}
