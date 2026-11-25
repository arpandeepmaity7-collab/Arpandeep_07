package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.CategoryTag
import com.example.data.model.PriorityLevel
import com.example.data.model.SubtaskItem
import com.example.data.model.TaskItem
import com.example.data.model.priorityLevel
import com.example.data.model.tagTone
import com.example.data.repository.FirestoreTaskRepository
import com.example.data.repository.TaskDraft
import com.example.ui.theme.FolioMonoSmall
import com.example.ui.workspace.DateFormatting
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorSheet(
    editingTask: TaskItem?,
    categories: List<CategoryTag>,
    onDismiss: () -> Unit,
    onSave: (TaskDraft) -> Unit,
    onOpenTagManager: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember(editingTask?.id) { mutableStateOf(editingTask?.title ?: "") }
    var notes by remember(editingTask?.id) { mutableStateOf(editingTask?.notes ?: "") }
    var priority by remember(editingTask?.id) {
        mutableStateOf(editingTask?.priorityLevel() ?: PriorityLevel.MEDIUM)
    }
    var dueAtMillis by remember(editingTask?.id) { mutableStateOf(editingTask?.dueAtMillis) }
    var reminderEnabled by remember(editingTask?.id) {
        mutableStateOf(editingTask?.reminderEnabled ?: false)
    }
    var reminderAtMillis by remember(editingTask?.id) {
        mutableStateOf(editingTask?.reminderAtMillis)
    }
    var selectedCategoryId by remember(editingTask?.id) {
        mutableStateOf(editingTask?.categoryId)
    }
    val subtasks = remember(editingTask?.id) {
        mutableStateListOf<SubtaskItem>().apply {
            addAll(editingTask?.subtasks ?: emptyList())
        }
    }
    var newSubtaskTitle by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf<String?>(null) }
    var showPermissionNotice by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            reminderEnabled = true
            if (dueAtMillis == null) {
                dueAtMillis = DateFormatting.todayDayMillis()
            }
            if (reminderAtMillis == null) {
                reminderAtMillis = DateFormatting.defaultReminderForDate(dueAtMillis!!)
            }
            showPermissionNotice = false
        } else {
            reminderEnabled = false
            showPermissionNotice = true
        }
    }

    fun enableReminderFlow() {
        val baseDate = dueAtMillis ?: DateFormatting.todayDayMillis()
        if (dueAtMillis == null) {
            dueAtMillis = baseDate
        }
        if (reminderAtMillis == null) {
            reminderAtMillis = DateFormatting.defaultReminderForDate(baseDate)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                reminderEnabled = true
            } else {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            reminderEnabled = true
        }
    }

    fun updateDueDatePreservingReminderTime(newDueMillis: Long?) {
        dueAtMillis = newDueMillis
        if (newDueMillis == null) {
            reminderEnabled = false
            reminderAtMillis = null
        } else if (reminderAtMillis != null) {
            val (hour, minute) = DateFormatting.extractHourMinute(reminderAtMillis!!)
            reminderAtMillis = DateFormatting.defaultReminderForDate(newDueMillis, hour, minute)
        } else if (reminderEnabled) {
            reminderAtMillis = DateFormatting.defaultReminderForDate(newDueMillis)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingTask == null) stringResource(R.string.new_task) else stringResource(R.string.edit_task),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close editor"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = {
                    if (it.length <= FirestoreTaskRepository.MAX_TITLE_LENGTH) {
                        title = it
                        titleError = null
                    }
                },
                label = { Text(stringResource(R.string.task_title_label)) },
                placeholder = { Text("e.g., Finalize quarterly editorial draft") },
                isError = titleError != null,
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(titleError ?: "")
                        Text(
                            text = "${title.length}/${FirestoreTaskRepository.MAX_TITLE_LENGTH}",
                            style = FolioMonoSmall
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = {
                    if (it.length <= FirestoreTaskRepository.MAX_NOTES_LENGTH) {
                        notes = it
                    }
                },
                label = { Text(stringResource(R.string.task_notes_label)) },
                placeholder = { Text("Context, links, or details…") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_notes_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Priority selector
            Text(
                text = "Priority Level",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PriorityLevel.entries.forEach { level ->
                    val selected = priority == level
                    val colors = priorityColors(level)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) colors.foreground else colors.background,
                        border = BorderStroke(
                            1.dp,
                            colors.foreground.copy(alpha = if (selected) 1f else 0.35f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { priority = level }
                            .testTag("priority_option_${level.name.lowercase()}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = level.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.surface else colors.foreground
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Due Date Presets
            Text(
                text = "Due Date",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DuePresetChip(
                    label = "Today",
                    selected = DateFormatting.isDueToday(dueAtMillis),
                    onClick = { updateDueDatePreservingReminderTime(DateFormatting.todayDayMillis()) }
                )
                DuePresetChip(
                    label = "Tomorrow",
                    selected = dueAtMillis != null && DateFormatting.formatDueBadge(dueAtMillis!!) == "Tomorrow",
                    onClick = { updateDueDatePreservingReminderTime(DateFormatting.tomorrowDayMillis()) }
                )
                DuePresetChip(
                    label = "Next Week",
                    selected = false,
                    onClick = { updateDueDatePreservingReminderTime(DateFormatting.nextWeekDayMillis()) }
                )
                DuePresetChip(
                    label = if (dueAtMillis != null) {
                        DateFormatting.formatDueBadge(dueAtMillis!!)
                    } else {
                        "Pick Date"
                    },
                    selected = dueAtMillis != null,
                    showCalendar = true,
                    onClick = { showDatePicker = true }
                )
                if (dueAtMillis != null) {
                    DuePresetChip(
                        label = "Clear",
                        selected = false,
                        onClick = { updateDueDatePreservingReminderTime(null) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reminder toggle & time
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                tint = if (reminderEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column {
                                Text(
                                    text = "Due-Date Reminder",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (reminderEnabled && reminderAtMillis != null) {
                                        "Alert at ${DateFormatting.formatTime(reminderAtMillis!!)}"
                                    } else {
                                        "Notify at 9:00 AM or custom time"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    enableReminderFlow()
                                } else {
                                    reminderEnabled = false
                                }
                            },
                            modifier = Modifier.testTag("reminder_switch")
                        )
                    }

                    if (reminderEnabled && reminderAtMillis != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Change Time (${DateFormatting.formatTime(reminderAtMillis!!)})")
                        }
                    }

                    if (showPermissionNotice) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.notification_permission_rationale),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Project Tag selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Project Tag",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onOpenTagManager) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manage Tags")
                }
            }

            if (categories.isEmpty()) {
                Text(
                    text = "No tags yet. Tap Manage Tags to organize tasks by project.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { tag ->
                        val selected = selectedCategoryId == tag.id
                        val tColors = tagToneColors(tag.tagTone())
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) tColors.foreground else tColors.background,
                            border = BorderStroke(
                                1.dp,
                                tColors.foreground.copy(alpha = if (selected) 1f else 0.35f)
                            ),
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedCategoryId = if (selected) null else tag.id
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) MaterialTheme.colorScheme.surface else tColors.foreground)
                                )
                                Text(
                                    text = tag.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.surface else tColors.foreground
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(14.dp))

            // Subtasks builder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subtasks",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${subtasks.size}/${FirestoreTaskRepository.MAX_SUBTASKS}",
                    style = FolioMonoSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            fun appendSubtask() {
                val clean = newSubtaskTitle.trim()
                if (clean.isNotEmpty() && subtasks.size < FirestoreTaskRepository.MAX_SUBTASKS) {
                    subtasks.add(
                        SubtaskItem(
                            id = "sub_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                            title = clean.take(FirestoreTaskRepository.MAX_TITLE_LENGTH),
                            completed = false
                        )
                    )
                    newSubtaskTitle = ""
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newSubtaskTitle,
                    onValueChange = {
                        if (it.length <= FirestoreTaskRepository.MAX_TITLE_LENGTH) {
                            newSubtaskTitle = it
                        }
                    },
                    placeholder = { Text("Add a step…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { appendSubtask() }),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("new_subtask_input")
                )
                OutlinedButton(
                    onClick = { appendSubtask() },
                    enabled = newSubtaskTitle.isNotBlank() && subtasks.size < FirestoreTaskRepository.MAX_SUBTASKS,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 52.dp)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add subtask")
                }
            }

            if (subtasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                subtasks.forEachIndexed { idx, sub ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = sub.completed,
                            onCheckedChange = { checked ->
                                subtasks[idx] = sub.copy(completed = checked)
                            }
                        )
                        Text(
                            text = sub.title,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { subtasks.removeAt(idx) }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Remove subtask",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val trimmedTitle = title.trim()
                    if (trimmedTitle.isEmpty()) {
                        titleError = "Please enter a task title"
                        return@Button
                    }
                    onSave(
                        TaskDraft(
                            title = trimmedTitle,
                            notes = notes.trim(),
                            completed = editingTask?.completed ?: false,
                            priority = priority,
                            dueAtMillis = dueAtMillis,
                            reminderAtMillis = if (reminderEnabled) reminderAtMillis else null,
                            reminderEnabled = reminderEnabled && reminderAtMillis != null,
                            categoryId = selectedCategoryId,
                            subtasks = subtasks.toList()
                        )
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_task_button")
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.save_task),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueAtMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val utcMillis = datePickerState.selectedDateMillis
                        if (utcMillis != null) {
                            // Convert UTC midnight from DatePicker to local end-of-day
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            val localCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                            }
                            updateDueDatePreservingReminderTime(
                                DateFormatting.endOfDayMillis(localCal.timeInMillis)
                            )
                        }
                        showDatePicker = false
                    }
                ) { Text("Set Date") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val (initHour, initMinute) = DateFormatting.extractHourMinute(
            reminderAtMillis ?: DateFormatting.defaultReminderForDate(dueAtMillis ?: DateFormatting.todayDayMillis())
        )
        val timeState = rememberTimePickerState(
            initialHour = initHour,
            initialMinute = initMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Reminder Time", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timeState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val baseDate = dueAtMillis ?: DateFormatting.todayDayMillis()
                        reminderAtMillis = DateFormatting.defaultReminderForDate(
                            baseDate,
                            timeState.hour,
                            timeState.minute
                        )
                        showTimePicker = false
                    }
                ) { Text("Set Time") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DuePresetChip(
    label: String,
    selected: Boolean,
    showCalendar: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (showCalendar) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
