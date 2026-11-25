package com.example.ui.workspace

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryTag
import com.example.data.model.PriorityLevel
import com.example.data.model.SmartFilter
import com.example.data.model.SortOption
import com.example.data.model.SubtaskItem
import com.example.data.model.TagTone
import com.example.data.model.TaskItem
import com.example.data.model.UiState
import com.example.data.model.priorityLevel
import com.example.data.repository.FirestoreTaskRepository
import com.example.data.repository.TaskDraft
import com.example.notifications.TaskReminderScheduler
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class ProgressSummary(
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val activeCount: Int = 0,
    val dueTodayCount: Int = 0,
    val overdueCount: Int = 0
) {
    val completionRatio: Float
        get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()
}

data class WorkspaceUiState(
    val tasksState: UiState<List<TaskItem>> = UiState.Loading,
    val filteredTasks: List<TaskItem> = emptyList(),
    val categories: List<CategoryTag> = emptyList(),
    val filterCounts: Map<SmartFilter, Int> = emptyMap(),
    val progressSummary: ProgressSummary = ProgressSummary(),
    val selectedFilter: SmartFilter = SmartFilter.ALL_ACTIVE,
    val selectedCategoryId: String? = null,
    val sortOption: SortOption = SortOption.DUE_DATE,
    val searchQuery: String = "",
    val selectedTaskId: String? = null
) {
    val selectedTask: TaskItem?
        get() {
            val tasks = (tasksState as? UiState.Success)?.data ?: return null
            return tasks.firstOrNull { it.id == selectedTaskId }
        }
}

class TaskWorkspaceViewModel(
    private val repository: FirestoreTaskRepository,
    private val reminderScheduler: TaskReminderScheduler,
    private val currentUserId: String,
    initialTaskId: String? = null
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(SmartFilter.ALL_ACTIVE)
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    private val _sortOption = MutableStateFlow(SortOption.DUE_DATE)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedTaskId = MutableStateFlow(initialTaskId)

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    val selectedFilter: StateFlow<SmartFilter> = _selectedFilter.asStateFlow()

    private val rawTasksState: StateFlow<UiState<List<TaskItem>>> =
        repository.observeTasks(currentUserId)
            .onEach { tasks ->
                reminderScheduler.syncTaskReminders(tasks)
            }
            .map<List<TaskItem>, UiState<List<TaskItem>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing tasks", rootError)
                emit(UiState.Error(rootError.message ?: "Unable to load tasks right now."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val rawCategories: StateFlow<List<CategoryTag>> =
        repository.observeCategories(currentUserId)
            .map { list -> list.sortedBy { it.name.lowercase() } }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing categories", firestoreError ?: error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    private val filterControlsFlow = combine(
        _selectedFilter,
        _selectedCategoryId,
        _sortOption,
        _searchQuery,
        _selectedTaskId
    ) { filter, categoryId, sort, query, selectedId ->
        FilterControls(filter, categoryId, sort, query, selectedId)
    }

    val uiState: StateFlow<WorkspaceUiState> = combine(
        rawTasksState,
        rawCategories,
        filterControlsFlow
    ) { tasksState, categories, controls ->
        val allTasks = (tasksState as? UiState.Success)?.data.orEmpty()
        val now = System.currentTimeMillis()

        val summary = ProgressSummary(
            totalCount = allTasks.size,
            completedCount = allTasks.count { it.completed },
            activeCount = allTasks.count { !it.completed },
            dueTodayCount = allTasks.count { !it.completed && DateFormatting.isDueToday(it.dueAtMillis, now) },
            overdueCount = allTasks.count { DateFormatting.isOverdue(it.dueAtMillis, it.completed, now) }
        )

        val filterCounts = mapOf(
            SmartFilter.ALL_ACTIVE to allTasks.count { !it.completed },
            SmartFilter.TODAY to allTasks.count { !it.completed && DateFormatting.isDueToday(it.dueAtMillis, now) },
            SmartFilter.UPCOMING to allTasks.count { !it.completed && DateFormatting.isUpcoming(it.dueAtMillis, now) },
            SmartFilter.OVERDUE to allTasks.count { DateFormatting.isOverdue(it.dueAtMillis, it.completed, now) },
            SmartFilter.HIGH_PRIORITY to allTasks.count { !it.completed && it.priorityLevel() == PriorityLevel.HIGH },
            SmartFilter.COMPLETED to allTasks.count { it.completed }
        )

        val validCategoryId = controls.categoryId?.takeIf { id -> categories.any { it.id == id } }
        val query = controls.searchQuery.trim().lowercase()

        val filtered = allTasks.filter { task ->
            val matchesSmart = when (controls.filter) {
                SmartFilter.ALL_ACTIVE -> !task.completed
                SmartFilter.TODAY -> !task.completed && DateFormatting.isDueToday(task.dueAtMillis, now)
                SmartFilter.UPCOMING -> !task.completed && DateFormatting.isUpcoming(task.dueAtMillis, now)
                SmartFilter.OVERDUE -> DateFormatting.isOverdue(task.dueAtMillis, task.completed, now)
                SmartFilter.HIGH_PRIORITY -> !task.completed && task.priorityLevel() == PriorityLevel.HIGH
                SmartFilter.COMPLETED -> task.completed
            }
            val matchesCategory = validCategoryId == null || task.categoryId == validCategoryId
            val matchesQuery = query.isEmpty() ||
                task.title.lowercase().contains(query) ||
                task.notes.lowercase().contains(query) ||
                task.subtasks.any { it.title.lowercase().contains(query) }
            matchesSmart && matchesCategory && matchesQuery
        }

        val sorted = when (controls.sortOption) {
            SortOption.DUE_DATE -> filtered.sortedWith(
                compareBy<TaskItem>(
                    { it.completed },
                    { it.dueAtMillis ?: Long.MAX_VALUE },
                    { it.priorityLevel().sortRank },
                    { -(it.updatedAt?.seconds ?: 0L) }
                )
            )
            SortOption.PRIORITY -> filtered.sortedWith(
                compareBy<TaskItem>(
                    { it.completed },
                    { it.priorityLevel().sortRank },
                    { it.dueAtMillis ?: Long.MAX_VALUE },
                    { -(it.updatedAt?.seconds ?: 0L) }
                )
            )
            SortOption.RECENT -> filtered.sortedWith(
                compareByDescending<TaskItem> { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            )
        }

        WorkspaceUiState(
            tasksState = tasksState,
            filteredTasks = sorted,
            categories = categories,
            filterCounts = filterCounts,
            progressSummary = summary,
            selectedFilter = controls.filter,
            selectedCategoryId = validCategoryId,
            sortOption = controls.sortOption,
            searchQuery = controls.searchQuery,
            selectedTaskId = controls.selectedTaskId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = WorkspaceUiState()
    )

    fun selectFilter(filter: SmartFilter) {
        _selectedFilter.value = filter
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = if (_selectedCategoryId.value == categoryId) null else categoryId
    }

    fun selectSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectTask(taskId: String?) {
        _selectedTaskId.value = taskId
    }

    fun createOrUpdateTask(
        editingTaskId: String?,
        draft: TaskDraft,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (editingTaskId == null) {
                val result = repository.createTask(currentUserId, draft)
                result.onSuccess { newId ->
                    _snackbarMessages.tryEmit(
                        if (draft.reminderEnabled && draft.reminderAtMillis != null) {
                            "Task added with reminder"
                        } else {
                            "Task added"
                        }
                    )
                    if (_selectedTaskId.value == null) {
                        _selectedTaskId.value = newId
                    }
                    onComplete(true)
                }.onFailure { err ->
                    _snackbarMessages.tryEmit(err.localizedMessage ?: "Couldn't save task")
                    onComplete(false)
                }
            } else {
                val result = repository.updateTask(currentUserId, editingTaskId, draft)
                result.onSuccess {
                    _snackbarMessages.tryEmit("Task updated")
                    onComplete(true)
                }.onFailure { err ->
                    _snackbarMessages.tryEmit(err.localizedMessage ?: "Couldn't update task")
                    onComplete(false)
                }
            }
        }
    }

    fun toggleTaskCompleted(task: TaskItem) {
        val nextCompleted = !task.completed
        viewModelScope.launch {
            val result = repository.toggleTaskCompleted(currentUserId, task.id, nextCompleted)
            result.onSuccess {
                _snackbarMessages.tryEmit(
                    if (nextCompleted) "Marked complete" else "Moved back to active"
                )
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "Couldn't update task status")
            }
        }
    }

    fun toggleSubtask(task: TaskItem, subtaskId: String) {
        val updated = task.subtasks.map { sub ->
            if (sub.id == subtaskId) sub.copy(completed = !sub.completed) else sub
        }
        viewModelScope.launch {
            val result = repository.updateSubtasks(currentUserId, task.id, updated)
            result.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "Couldn't update subtask")
            }
        }
    }

    fun addSubtaskToTask(task: TaskItem, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        if (task.subtasks.size >= FirestoreTaskRepository.MAX_SUBTASKS) {
            _snackbarMessages.tryEmit("Maximum ${FirestoreTaskRepository.MAX_SUBTASKS} subtasks per task")
            return
        }
        val newSub = SubtaskItem(
            id = "sub_${UUID.randomUUID().toString().replace("-", "").take(12)}",
            title = trimmed.take(FirestoreTaskRepository.MAX_TITLE_LENGTH),
            completed = false
        )
        viewModelScope.launch {
            val result = repository.updateSubtasks(currentUserId, task.id, task.subtasks + newSub)
            result.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "Couldn't add subtask")
            }
        }
    }

    fun removeSubtaskFromTask(task: TaskItem, subtaskId: String) {
        val updated = task.subtasks.filterNot { it.id == subtaskId }
        viewModelScope.launch {
            val result = repository.updateSubtasks(currentUserId, task.id, updated)
            result.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "Couldn't remove subtask")
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            val result = repository.deleteTask(currentUserId, taskId)
            result.onSuccess {
                if (_selectedTaskId.value == taskId) {
                    _selectedTaskId.value = null
                }
                _snackbarMessages.tryEmit("Task deleted")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "Couldn't delete task")
            }
        }
    }

    fun createCategory(name: String, tone: TagTone, onCreated: (String?) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.createCategory(currentUserId, name, tone)
            result.onSuccess { id ->
                _snackbarMessages.tryEmit("Tag \"$name\" created")
                onCreated(id)
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "Couldn't create tag")
                onCreated(null)
            }
        }
    }

    fun updateCategory(categoryId: String, name: String, tone: TagTone) {
        viewModelScope.launch {
            val result = repository.updateCategory(currentUserId, categoryId, name, tone)
            result.onSuccess {
                _snackbarMessages.tryEmit("Tag updated")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "Couldn't update tag")
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        val allTasks = (rawTasksState.value as? UiState.Success)?.data.orEmpty()
        val affectedIds = allTasks.filter { it.categoryId == categoryId }.map { it.id }
        viewModelScope.launch {
            val result = repository.deleteCategory(currentUserId, categoryId, affectedIds)
            result.onSuccess {
                if (_selectedCategoryId.value == categoryId) {
                    _selectedCategoryId.value = null
                }
                _snackbarMessages.tryEmit("Tag removed")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "Couldn't delete tag")
            }
        }
    }

    private data class FilterControls(
        val filter: SmartFilter,
        val categoryId: String?,
        val sortOption: SortOption,
        val searchQuery: String,
        val selectedTaskId: String?
    )

    private companion object {
        const val TAG = "TaskWorkspaceVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
