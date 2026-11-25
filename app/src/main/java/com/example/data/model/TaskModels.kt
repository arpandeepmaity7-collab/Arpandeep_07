package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PriorityLevel(val label: String, val sortRank: Int) {
    HIGH("High", 0),
    MEDIUM("Medium", 1),
    LOW("Low", 2);

    companion object {
        fun fromRaw(raw: String?): PriorityLevel =
            entries.firstOrNull { it.name == raw } ?: MEDIUM
    }
}

enum class TagTone(val key: String, val label: String) {
    TERRACOTTA("terracotta", "Terracotta"),
    OCHRE("ochre", "Ochre"),
    SAGE("sage", "Sage"),
    INDIGO("indigo", "Indigo"),
    PLUM("plum", "Plum"),
    CHARCOAL("charcoal", "Ink");

    companion object {
        fun fromKey(raw: String?): TagTone =
            entries.firstOrNull { it.key == raw } ?: CHARCOAL
    }
}

enum class SmartFilter(val label: String) {
    ALL_ACTIVE("Active"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    OVERDUE("Overdue"),
    HIGH_PRIORITY("High Priority"),
    COMPLETED("Completed")
}

enum class SortOption(val label: String) {
    DUE_DATE("Due Date"),
    PRIORITY("Priority"),
    RECENT("Recently Updated")
}

@Keep
@IgnoreExtraProperties
data class SubtaskItem(
    val id: String = "",
    val title: String = "",
    val completed: Boolean = false
)

@Keep
@IgnoreExtraProperties
data class TaskItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val notes: String = "",
    val completed: Boolean = false,
    val priority: String = PriorityLevel.MEDIUM.name,
    val dueAtMillis: Long? = null,
    val reminderAtMillis: Long? = null,
    val reminderEnabled: Boolean = false,
    val categoryId: String? = null,
    val subtasks: List<SubtaskItem> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CategoryTag(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val tone: String = TagTone.TERRACOTTA.key,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

fun TaskItem.priorityLevel(): PriorityLevel = PriorityLevel.fromRaw(priority)

fun TaskItem.subtaskStats(): Pair<Int, Int> {
    if (subtasks.isEmpty()) return 0 to 0
    val done = subtasks.count { it.completed }
    return done to subtasks.size
}

fun TaskItem.progressFraction(): Float {
    if (subtasks.isEmpty()) return if (completed) 1f else 0f
    val done = subtasks.count { it.completed }
    return done.toFloat() / subtasks.size.toFloat()
}

fun CategoryTag.tagTone(): TagTone = TagTone.fromKey(tone)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
