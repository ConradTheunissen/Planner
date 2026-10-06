package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.EntryStatus
import com.example.ui.components.AddEntryDialog
import com.example.ui.components.AiSummaryDialog
import com.example.ui.components.ComposeEmailDialog
import com.example.ui.components.ConnectedAccountCard
import com.example.ui.components.DepartmentOverviewCard
import com.example.ui.components.DriveFileCard
import com.example.ui.components.EntryCard
import com.example.ui.components.MailMessageCard
import com.example.ui.components.ReviewEntryDialog
import com.example.ui.components.TopBarHeader
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.OmniSyncViewModel

@Composable
fun MainScreen(
    viewModel: OmniSyncViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val departments by viewModel.departments.collectAsStateWithLifecycle()
    val filteredEntries by viewModel.filteredEntries.collectAsStateWithLifecycle()
    val pendingReviewEntries by viewModel.pendingReviewEntries.collectAsStateWithLifecycle()
    val allDriveFiles by viewModel.allDriveFiles.collectAsStateWithLifecycle()
    val allMailMessages by viewModel.allMailMessages.collectAsStateWithLifecycle()

    val selectedDeptFilter by viewModel.selectedDeptFilter.collectAsStateWithLifecycle()
    val selectedStatusFilter by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val addEntryState by viewModel.addEntryState.collectAsStateWithLifecycle()
    val reviewEntryState by viewModel.reviewEntryState.collectAsStateWithLifecycle()
    val composerState by viewModel.composerState.collectAsStateWithLifecycle()
    val summarySheetState by viewModel.summarySheetState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    var workspaceSubTab by remember { mutableIntStateOf(0) } // 0: Drive Files, 1: Gmail Inbox

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopBarHeader(
                    isSyncing = isSyncing,
                    onSyncClick = { viewModel.syncWorkspace() }
                )

                ConnectedAccountCard(
                    onComposeClick = { viewModel.openAddEntry() },
                    onUploadClick = { viewModel.openComposer() }
                )

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search departments, entries, or files...", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("main_search_input")
                )

                // Department & Status Filter Row (Shown in ENTRIES tab)
                if (selectedTab == AppTab.ENTRIES) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedDeptFilter == "ALL" || selectedDeptFilter == null,
                                onClick = { viewModel.setDeptFilter("ALL") },
                                label = { Text("All Depts", fontSize = 11.sp) }
                            )
                        }
                        items(departments, key = { it.id }) { dept ->
                            FilterChip(
                                selected = selectedDeptFilter == dept.id,
                                onClick = { viewModel.setDeptFilter(dept.id) },
                                label = { Text(dept.code, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == AppTab.DEPARTMENTS,
                    onClick = { viewModel.selectTab(AppTab.DEPARTMENTS) },
                    icon = {
                        Icon(
                            if (selectedTab == AppTab.DEPARTMENTS) Icons.Filled.Business else Icons.Outlined.Business,
                            contentDescription = "Departments"
                        )
                    },
                    label = { Text("Departments") },
                    modifier = Modifier.testTag("tab_departments")
                )

                NavigationBarItem(
                    selected = selectedTab == AppTab.ENTRIES,
                    onClick = { viewModel.selectTab(AppTab.ENTRIES) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (filteredEntries.isNotEmpty()) {
                                    Badge { Text("${filteredEntries.size}") }
                                }
                            }
                        ) {
                            Icon(
                                if (selectedTab == AppTab.ENTRIES) Icons.AutoMirrored.Filled.ListAlt else Icons.AutoMirrored.Outlined.ListAlt,
                                contentDescription = "Entries"
                            )
                        }
                    },
                    label = { Text("Entries") },
                    modifier = Modifier.testTag("tab_entries")
                )

                NavigationBarItem(
                    selected = selectedTab == AppTab.REVIEWS,
                    onClick = { viewModel.selectTab(AppTab.REVIEWS) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (pendingReviewEntries.isNotEmpty()) {
                                    Badge { Text("${pendingReviewEntries.size}") }
                                }
                            }
                        ) {
                            Icon(
                                if (selectedTab == AppTab.REVIEWS) Icons.Filled.RateReview else Icons.Outlined.RateReview,
                                contentDescription = "Review Queue"
                            )
                        }
                    },
                    label = { Text("Reviews") },
                    modifier = Modifier.testTag("tab_reviews")
                )

                NavigationBarItem(
                    selected = selectedTab == AppTab.WORKSPACE,
                    onClick = { viewModel.selectTab(AppTab.WORKSPACE) },
                    icon = {
                        Icon(
                            if (selectedTab == AppTab.WORKSPACE) Icons.Filled.CloudDone else Icons.Outlined.CloudDone,
                            contentDescription = "Drive & Mail"
                        )
                    },
                    label = { Text("Drive & Mail") },
                    modifier = Modifier.testTag("tab_workspace")
                )
            }
        },
        floatingActionButton = {
            when (selectedTab) {
                AppTab.DEPARTMENTS, AppTab.ENTRIES -> {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.openAddEntry() },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("New Entry") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_add_entry")
                    )
                }
                AppTab.REVIEWS -> {
                    // Reviews tab has inline review actions on each card
                }
                AppTab.WORKSPACE -> {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.openComposer() },
                        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        text = { Text("Compose") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                // TAB 1: DEPARTMENTS OVERVIEW
                AppTab.DEPARTMENTS -> {
                    LazyColumn(
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Text(
                                text = "Enterprise Departments",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }

                        items(departments, key = { it.id }) { dept ->
                            DepartmentOverviewCard(
                                department = dept,
                                isSelected = selectedDeptFilter == dept.id,
                                onClick = {
                                    viewModel.setDeptFilter(dept.id)
                                    viewModel.selectTab(AppTab.ENTRIES)
                                },
                                onAddEntryClick = {
                                    viewModel.openAddEntry(dept.id)
                                }
                            )
                        }

                        // Recent submissions quick-peek
                        item {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Recent Department Submissions",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }

                        items(filteredEntries.take(4), key = { it.id }) { entry ->
                            EntryCard(
                                entry = entry,
                                onEntryClick = { viewModel.openReviewDialog(entry) },
                                onReviewClick = { viewModel.openReviewDialog(entry) },
                                onAiAnalyzeClick = { viewModel.analyzeEntryWithAi(entry) }
                            )
                        }
                    }
                }

                // TAB 2: ENTRIES LIST
                AppTab.ENTRIES -> {
                    if (filteredEntries.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.AutoMirrored.Filled.ListAlt,
                            title = "No entries found",
                            subtitle = if (searchQuery.isNotBlank()) "No submissions match '$searchQuery'" else "Create an entry for this department with Google Drive documentation attached.",
                            actionLabel = "Create New Entry",
                            onAction = { viewModel.openAddEntry() }
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredEntries, key = { it.id }) { entry ->
                                EntryCard(
                                    entry = entry,
                                    onEntryClick = { viewModel.openReviewDialog(entry) },
                                    onReviewClick = { viewModel.openReviewDialog(entry) },
                                    onAiAnalyzeClick = { viewModel.analyzeEntryWithAi(entry) }
                                )
                            }
                        }
                    }
                }

                // TAB 3: REVIEW QUEUE
                AppTab.REVIEWS -> {
                    if (pendingReviewEntries.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.CheckCircle,
                            title = "Review queue is clear!",
                            subtitle = "All department submissions have been evaluated and approved.",
                            actionLabel = "View All Entries",
                            onAction = { viewModel.selectTab(AppTab.ENTRIES) }
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${pendingReviewEntries.size} Items Awaiting Review",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            items(pendingReviewEntries, key = { it.id }) { entry ->
                                EntryCard(
                                    entry = entry,
                                    onEntryClick = { viewModel.openReviewDialog(entry) },
                                    onReviewClick = { viewModel.openReviewDialog(entry) },
                                    onAiAnalyzeClick = { viewModel.analyzeEntryWithAi(entry) }
                                )
                            }
                        }
                    }
                }

                // TAB 4: WORKSPACE (DRIVE & GMAIL DIRECT)
                AppTab.WORKSPACE -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TabRow(
                            selectedTabIndex = workspaceSubTab,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Tab(
                                selected = workspaceSubTab == 0,
                                onClick = { workspaceSubTab = 0 },
                                text = { Text("Google Drive Files (${allDriveFiles.size})") }
                            )
                            Tab(
                                selected = workspaceSubTab == 1,
                                onClick = { workspaceSubTab = 1 },
                                text = { Text("Gmail Messages (${allMailMessages.size})") }
                            )
                        }

                        if (workspaceSubTab == 0) {
                            LazyColumn(
                                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(allDriveFiles, key = { it.id }) { file ->
                                    DriveFileCard(
                                        file = file,
                                        onFileClick = { },
                                        onStarClick = { },
                                        onSummarizeClick = { viewModel.summarizeFileWithAi(file) },
                                        onAttachClick = { viewModel.openComposer(initialFile = file) }
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(allMailMessages, key = { it.id }) { mail ->
                                    MailMessageCard(
                                        mail = mail,
                                        onMailClick = { },
                                        onStarClick = { },
                                        onSummarizeClick = { viewModel.summarizeEmailWithAi(mail) },
                                        onReplyClick = {
                                            viewModel.openComposer(
                                                prefillRecipient = mail.senderEmail,
                                                prefillSubject = "Re: ${mail.subject}"
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialogs
    AddEntryDialog(
        state = addEntryState,
        departments = departments,
        driveFiles = allDriveFiles,
        onDismiss = { viewModel.closeAddEntry() },
        onDeptChange = { viewModel.updateAddEntryDept(it) },
        onTitleChange = { viewModel.updateAddEntryTitle(it) },
        onDescriptionChange = { viewModel.updateAddEntryDescription(it) },
        onPriorityChange = { viewModel.updateAddEntryPriority(it) },
        onSelectDriveFile = { viewModel.selectDriveFileForEntry(it) },
        onToggleSendNotice = { viewModel.toggleAddEntrySendNotice(it) },
        onSubmit = { viewModel.submitNewEntry() }
    )

    ReviewEntryDialog(
        state = reviewEntryState,
        onDismiss = { viewModel.closeReviewDialog() },
        onNotesChange = { viewModel.updateReviewNotes(it) },
        onToggleSendEmail = { viewModel.toggleReviewSendEmail(it) },
        onAiAnalyze = { viewModel.analyzeEntryWithAi(it) },
        onDecision = { viewModel.submitReviewDecision(it) }
    )

    AiSummaryDialog(
        state = summarySheetState,
        onDismiss = { viewModel.closeSummarySheet() }
    )

    ComposeEmailDialog(
        state = composerState,
        allDriveFiles = allDriveFiles,
        onDismiss = { viewModel.closeComposer() },
        onRecipientChange = { viewModel.updateComposerRecipient(it) },
        onSubjectChange = { viewModel.updateComposerSubject(it) },
        onBodyChange = { viewModel.updateComposerBody(it) },
        onAttachFile = { viewModel.attachFileToComposer(it) },
        onRemoveAttachment = { viewModel.removeAttachmentFromComposer(it) },
        onPolishDraft = { viewModel.polishDraftWithAi(it) },
        onSend = { viewModel.sendEmail() }
    )
}

@Composable
fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onAction,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(actionLabel, fontWeight = FontWeight.SemiBold)
        }
    }
}
