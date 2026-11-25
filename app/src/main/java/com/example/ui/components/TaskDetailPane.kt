package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryTag
import com.example.data.model.TaskItem
import com.example.data.model.priorityLevel
import com.example.data.model.progressFraction
import com.example.data.model.subtaskStats
import com.example.data.model.tagTone
import com.example.data.repository.FirestoreTaskRepository
import com.example.ui.theme.FolioMonoMedium
import com.example.ui.theme.FolioMonoSmall
import com.example.ui.workspace.DateFormatting

@Composable
fun TaskDetailPane(
    task: TaskItem,
    category: CategoryTag?,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleSubtask: (String) -> Unit,
    onAddSubtask: (String) -> Unit,
    onRemoveSubtask: (String) -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val priority = task.priorityLevel()
    val pColors = priorityColors(priority)
    val isOverdue = DateFormatting.isOverdue(task.dueAtMillis, task.completed)
    val (subDone, subTotal) = task.subtaskStats()
    val animatedProgress by animateFloatAsState(
        targetValue = task.progressFraction(),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "detail_progress"
    )

    var newSubtaskTitle by remember(task.id) { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = pColors.background
            ) {
                Text(
                    text = "${priority.label} Priority",
                    style = MaterialTheme.typography.labelMedium,
                    color = pColors.foreground,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("detail_edit_button")
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit task")
                }
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.testTag("detail_delete_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete task",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                if (onClose != null) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close detail")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = task.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
            ),
            color = if (task.completed) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )

        if (task.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = task.notes,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Due, Reminder, Tag badges
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            task.dueAtMillis?.let { due ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = if (isOverdue) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isOverdue) {
                            "Overdue (${DateFormatting.formatDueBadge(due)})"
                        } else {
                            "Due ${DateFormatting.formatDueBadge(due)}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isOverdue) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (task.reminderEnabled && task.reminderAtMillis != null && !task.completed) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Reminder at ${DateFormatting.formatTime(task.reminderAtMillis)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (category != null) {
                val tColors = tagToneColors(category.tagTone())
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Label,
                        contentDescription = null,
                        tint = tColors.foreground,
                        modifier = Modifier.size(16.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = tColors.background
                    ) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = tColors.foreground,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onToggleComplete,
            shape = RoundedCornerShape(12.dp),
            colors = if (task.completed) {
                ButtonDefaults.outlinedButtonColors()
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            },
            border = if (task.completed) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("detail_toggle_complete_button")
        ) {
            Icon(
                imageVector = if (task.completed) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (task.completed) "Completed · Tap to Reopen" else "Mark Task Complete")
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Subtasks & Progress",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (subTotal > 0) "$subDone / $subTotal" else "No steps yet",
                style = FolioMonoMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (subTotal > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (subDone == subTotal) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        task.subtasks.forEach { sub ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = sub.completed,
                    onCheckedChange = { onToggleSubtask(sub.id) }
                )
                Text(
                    text = sub.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (sub.completed) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (sub.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onRemoveSubtask(sub.id) }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Remove subtask",
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        if (task.subtasks.size < FirestoreTaskRepository.MAX_SUBTASKS) {
            Spacer(modifier = Modifier.height(10.dp))
            fun submitNewSubtask() {
                val clean = newSubtaskTitle.trim()
                if (clean.isNotEmpty()) {
                    onAddSubtask(clean)
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
                    placeholder = { Text("Add a subtask…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitNewSubtask() }),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = { submitNewSubtask() },
                    enabled = newSubtaskTitle.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 52.dp)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add subtask")
                }
            }
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Subtask limit (${FirestoreTaskRepository.MAX_SUBTASKS}) reached.",
                style = FolioMonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete task?", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("“${task.title}” and its subtasks will be permanently removed from your synced notebook.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.secondary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
