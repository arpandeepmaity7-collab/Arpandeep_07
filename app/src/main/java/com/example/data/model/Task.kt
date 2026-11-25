package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.Date

enum class Priority {
    HIGH,
    MEDIUM,
    LOW;

    companion object {
        fun fromLevel(level: PriorityLevel): Priority = when (level) {
            PriorityLevel.HIGH -> HIGH
            PriorityLevel.MEDIUM -> MEDIUM
            PriorityLevel.LOW -> LOW
        }
    }
}

@Keep
@IgnoreExtraProperties
data class Task(
    @DocumentId val id: String = "",
    val title: String = "",
    val priority: Priority = Priority.MEDIUM,
    val dueDate: Timestamp? = null,
    val subtasks: List<String> = emptyList(),
    val progress: Float = 0f,
    val tags: List<String> = emptyList()
)

fun TaskItem.toTask(categoriesById: Map<String, CategoryTag> = emptyMap()): Task {
    val resolvedTags = categoryId?.let { id ->
        listOfNotNull(categoriesById[id]?.name)
    } ?: emptyList()

    return Task(
        id = id,
        title = title,
        priority = Priority.fromLevel(priorityLevel()),
        dueDate = dueAtMillis?.let { Timestamp(Date(it)) },
        subtasks = subtasks.map { it.title },
        progress = progressFraction(),
        tags = resolvedTags
    )
}
