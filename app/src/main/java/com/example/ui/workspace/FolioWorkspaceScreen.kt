package com.example.ui.workspace

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.auth.signOut
import com.example.data.model.SmartFilter
import com.example.data.model.SortOption
import com.example.data.model.TaskItem
import com.example.data.model.UiState
import com.example.ui.components.AccountDialog
import com.example.ui.components.CategoryFilterStrip
import com.example.ui.components.EditorialProgressBanner
import com.example.ui.components.SmartFilterStrip
import com.example.ui.components.TagManagerDialog
import com.example.ui.components.TaskCard
import com.example.ui.components.TaskDetailPane
import com.example.ui.components.TaskEditorSheet
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolioWorkspaceScreen(
    viewModel: TaskWorkspaceViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val currentUser = Firebase.auth.currentUser

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSearch by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showTagManager by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var editorOpen by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val categoriesById = remember(uiState.categories) {
        uiState.categories.associateBy { it.id }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideLayout = maxWidth >= 760.dp

        if (!isWideLayout && uiState.selectedTask != null) {
            BackHandler { viewModel.selectTask(null) }
        }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingTask = null
                        editorOpen = true
                    },
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.new_task)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("add_task_fab")
                )
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Primary Task Notebook Feed
                Column(
                    modifier = Modifier
                        .weight(if (isWideLayout) 0.56f else 1f)
                        .fillMaxHeight()
                ) {
                    // Editorial Masthead
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = DateFormatting.formatEditorialHeaderDate().uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        showSearch = !showSearch
                                        if (!showSearch) viewModel.updateSearchQuery("")
                                    },
                                    modifier = Modifier.testTag("search_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (showSearch) Icons.Outlined.Close else Icons.Outlined.Search,
                                        contentDescription = "Search tasks"
                                    )
                                }

                                Box {
                                    IconButton(
                                        onClick = { showSortMenu = true },
                                        modifier = Modifier.testTag("sort_menu_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.Sort,
                                            contentDescription = "Sort tasks"
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false }
                                    ) {
                                        SortOption.entries.forEach { option ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = option.label,
                                                        fontWeight = if (uiState.sortOption == option) {
                                                            FontWeight.Bold
                                                        } else {
                                                            FontWeight.Normal
                                                        }
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.selectSortOption(option)
                                                    showSortMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                val initial = currentUser?.displayName?.firstOrNull()?.uppercase()
                                    ?: currentUser?.email?.firstOrNull()?.uppercase()
                                    ?: "F"
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .clickable { showAccountDialog = true }
                                        .testTag("account_avatar_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initial,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = showSearch) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = viewModel::updateSearchQuery,
                                placeholder = { Text(stringResource(R.string.search_tasks_hint)) },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Outlined.Search, contentDescription = null)
                                },
                                trailingIcon = {
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                            Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .testTag("search_input")
                            )
                        }
                    }

                    EditorialProgressBanner(
                        summary = uiState.progressSummary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SmartFilterStrip(
                        selectedFilter = uiState.selectedFilter,
                        filterCounts = uiState.filterCounts,
                        onSelectFilter = viewModel::selectFilter
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CategoryFilterStrip(
                        categories = uiState.categories,
                        selectedCategoryId = uiState.selectedCategoryId,
                        onSelectCategory = viewModel::selectCategory,
                        onManageTags = { showTagManager = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    when (val state = uiState.tasksState) {
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                            }
                        }

                        is UiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Sync Interrupted",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        is UiState.Success -> {
                            if (uiState.filteredTasks.isEmpty()) {
                                EmptyTasksView(
                                    hasAnyTasks = state.data.isNotEmpty(),
                                    selectedFilter = uiState.selectedFilter,
                                    hasTagFilter = uiState.selectedCategoryId != null,
                                    hasSearch = uiState.searchQuery.isNotBlank(),
                                    onClearFilters = {
                                        viewModel.selectFilter(SmartFilter.ALL_ACTIVE)
                                        viewModel.selectCategory(null)
                                        viewModel.updateSearchQuery("")
                                    },
                                    onAddTask = {
                                        editingTask = null
                                        editorOpen = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentPadding = PaddingValues(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 12.dp,
                                        bottom = 96.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(
                                        items = uiState.filteredTasks,
                                        key = { it.id }
                                    ) { task ->
                                        TaskCard(
                                            task = task,
                                            category = task.categoryId?.let { categoriesById[it] },
                                            isSelected = isWideLayout && uiState.selectedTaskId == task.id,
                                            onToggleComplete = { viewModel.toggleTaskCompleted(task) },
                                            onToggleSubtask = { subId ->
                                                viewModel.toggleSubtask(task, subId)
                                            },
                                            onClick = { viewModel.selectTask(task.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Expanded / Tablet Side Detail Pane
                if (isWideLayout) {
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Box(
                        modifier = Modifier
                            .weight(0.44f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        val selected = uiState.selectedTask
                        if (selected != null) {
                            TaskDetailPane(
                                task = selected,
                                category = selected.categoryId?.let { categoriesById[it] },
                                onToggleComplete = { viewModel.toggleTaskCompleted(selected) },
                                onEdit = {
                                    editingTask = selected
                                    editorOpen = true
                                },
                                onDelete = { viewModel.deleteTask(selected.id) },
                                onToggleSubtask = { subId -> viewModel.toggleSubtask(selected, subId) },
                                onAddSubtask = { title -> viewModel.addSubtaskToTask(selected, title) },
                                onRemoveSubtask = { subId -> viewModel.removeSubtaskFromTask(selected, subId) }
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Select a task",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Inspect notes, manage subtasks, and adjust reminders here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Compact Screen Detail Bottom Sheet
        val compactSelectedTask = if (!isWideLayout) uiState.selectedTask else null
        if (compactSelectedTask != null) {
            val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { viewModel.selectTask(null) },
                sheetState = detailSheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                TaskDetailPane(
                    task = compactSelectedTask,
                    category = compactSelectedTask.categoryId?.let { categoriesById[it] },
                    onToggleComplete = { viewModel.toggleTaskCompleted(compactSelectedTask) },
                    onEdit = {
                        editingTask = compactSelectedTask
                        editorOpen = true
                    },
                    onDelete = { viewModel.deleteTask(compactSelectedTask.id) },
                    onToggleSubtask = { subId -> viewModel.toggleSubtask(compactSelectedTask, subId) },
                    onAddSubtask = { title -> viewModel.addSubtaskToTask(compactSelectedTask, title) },
                    onRemoveSubtask = { subId -> viewModel.removeSubtaskFromTask(compactSelectedTask, subId) },
                    onClose = { viewModel.selectTask(null) },
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        }

        if (editorOpen) {
            TaskEditorSheet(
                editingTask = editingTask,
                categories = uiState.categories,
                onDismiss = {
                    editorOpen = false
                    editingTask = null
                },
                onSave = { draft ->
                    val targetId = editingTask?.id
                    viewModel.createOrUpdateTask(targetId, draft) { success ->
                        if (success) {
                            editorOpen = false
                            editingTask = null
                        }
                    }
                },
                onOpenTagManager = { showTagManager = true }
            )
        }

        if (showTagManager) {
            TagManagerDialog(
                categories = uiState.categories,
                onDismiss = { showTagManager = false },
                onCreateTag = { name, tone -> viewModel.createCategory(name, tone) },
                onUpdateTag = { id, name, tone -> viewModel.updateCategory(id, name, tone) },
                onDeleteTag = { id -> viewModel.deleteCategory(id) }
            )
        }

        if (showAccountDialog) {
            AccountDialog(
                displayName = currentUser?.displayName,
                email = currentUser?.email,
                onDismiss = { showAccountDialog = false },
                onSignOut = {
                    showAccountDialog = false
                    signOut(
                        context = context,
                        credentialManager = credentialManager,
                        scope = scope
                    )
                }
            )
        }
    }
}

@Composable
private fun EmptyTasksView(
    hasAnyTasks: Boolean,
    selectedFilter: SmartFilter,
    hasTagFilter: Boolean,
    hasSearch: Boolean,
    onClearFilters: () -> Unit,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_editorial_desk_1795607260556),
                contentDescription = "Empty notebook page",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when {
                    !hasAnyTasks -> "Start your first folio"
                    hasSearch -> "No matching tasks"
                    else -> "Nothing in ${selectedFilter.label}"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when {
                    !hasAnyTasks -> "Add a task with a due date, priority tier, or checklist of subtasks."
                    hasTagFilter || hasSearch || selectedFilter != SmartFilter.ALL_ACTIVE ->
                        "Try clearing your active filters or add a new task."
                    else -> "You’re all caught up on active work."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (hasAnyTasks && (hasTagFilter || hasSearch || selectedFilter != SmartFilter.ALL_ACTIVE)) {
                OutlinedButton(
                    onClick = onClearFilters,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Show All Active")
                }
            } else {
                OutlinedButton(
                    onClick = onAddTask,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create Task")
                }
            }
        }
    }
}
