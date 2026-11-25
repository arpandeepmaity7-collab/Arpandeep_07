package com.example

import com.example.data.model.CategoryTag
import com.example.data.model.Priority
import com.example.data.model.PriorityLevel
import com.example.data.model.SubtaskItem
import com.example.data.model.Task
import com.example.data.model.TaskItem
import com.example.data.model.toTask
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class TaskModelTest {
    @Test
    fun task_defaults_are_valid_for_firestore_deserialization() {
        val task = Task()
        assertEquals("", task.id)
        assertEquals("", task.title)
        assertEquals(Priority.MEDIUM, task.priority)
        assertNull(task.dueDate)
        assertEquals(emptyList<String>(), task.subtasks)
        assertEquals(0f, task.progress, 0.0001f)
        assertEquals(emptyList<String>(), task.tags)
    }

    @Test
    fun task_holds_all_requested_fields() {
        val due = Timestamp(Date(1_800_000_000_000L))
        val task = Task(
            id = "task_1",
            title = "Prepare release notes",
            priority = Priority.HIGH,
            dueDate = due,
            subtasks = listOf("Draft changelog", "Verify screenshots"),
            progress = 0.5f,
            tags = listOf("Release", "Editorial")
        )
        assertEquals("Prepare release notes", task.title)
        assertEquals(Priority.HIGH, task.priority)
        assertEquals(due, task.dueDate)
        assertEquals(listOf("Draft changelog", "Verify screenshots"), task.subtasks)
        assertEquals(0.5f, task.progress, 0.0001f)
        assertEquals(listOf("Release", "Editorial"), task.tags)
    }

    @Test
    fun taskItem_maps_cleanly_to_Task() {
        val item = TaskItem(
            id = "t_42",
            title = "Review quarterly folio",
            priority = PriorityLevel.LOW.name,
            dueAtMillis = 1_795_000_000_000L,
            categoryId = "cat_1",
            subtasks = listOf(
                SubtaskItem(id = "s1", title = "Step 1", completed = true),
                SubtaskItem(id = "s2", title = "Step 2", completed = false)
            )
        )
        val categories = mapOf("cat_1" to CategoryTag(id = "cat_1", name = "Studio"))

        val mapped = item.toTask(categories)
        assertEquals("t_42", mapped.id)
        assertEquals("Review quarterly folio", mapped.title)
        assertEquals(Priority.LOW, mapped.priority)
        assertEquals(1_795_000_000L, mapped.dueDate?.seconds)
        assertEquals(listOf("Step 1", "Step 2"), mapped.subtasks)
        assertEquals(0.5f, mapped.progress, 0.0001f)
        assertEquals(listOf("Studio"), mapped.tags)
    }
}
