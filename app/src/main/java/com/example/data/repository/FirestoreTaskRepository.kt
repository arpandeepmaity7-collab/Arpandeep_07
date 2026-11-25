package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CategoryTag
import com.example.data.model.PriorityLevel
import com.example.data.model.SubtaskItem
import com.example.data.model.TagTone
import com.example.data.model.TaskItem
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class TaskDraft(
    val title: String,
    val notes: String,
    val completed: Boolean,
    val priority: PriorityLevel,
    val dueAtMillis: Long?,
    val reminderAtMillis: Long?,
    val reminderEnabled: Boolean,
    val categoryId: String?,
    val subtasks: List<SubtaskItem>
)

class FirestoreTaskRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireSignedInUid(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in with Google to sync tasks.")
        check(uid == expectedUserId) { "Signed-in account changed; cancelling write." }
        return uid
    }

    fun observeTasks(userId: String): Flow<List<TaskItem>> = flow {
        val uid = requireSignedInUid(userId)
        val path = "users/$uid/tasks"
        emitAll(
            db.collection("users").document(uid).collection("tasks")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TaskItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeCategories(userId: String): Flow<List<CategoryTag>> = flow {
        val uid = requireSignedInUid(userId)
        val path = "users/$uid/categories"
        emitAll(
            db.collection("users").document(uid).collection("categories")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CategoryTag::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createTask(userId: String, draft: TaskDraft): Result<String> {
        var docPath = "users/$userId/tasks"
        return try {
            val uid = requireSignedInUid(userId)
            val validatedTitle = validateTitle(draft.title)
            val validatedNotes = validateNotes(draft.notes)
            val validatedSubtasks = serializeSubtasks(draft.subtasks)
            val cleanCategoryId = draft.categoryId?.takeIf { isValidFirestoreId(it) }
            val cleanDue = draft.dueAtMillis?.takeIf { it >= 0L }
            val cleanReminder = if (draft.reminderEnabled && draft.reminderAtMillis != null && draft.reminderAtMillis >= 0L) {
                draft.reminderAtMillis
            } else {
                null
            }
            val reminderActive = draft.reminderEnabled && cleanReminder != null && !draft.completed

            val docRef = db.collection("users").document(uid).collection("tasks").document()
            docPath = docRef.path

            val payload = buildMap<String, Any?> {
                put("userId", uid)
                put("title", validatedTitle)
                put("notes", validatedNotes)
                put("completed", draft.completed)
                put("priority", draft.priority.name)
                put("dueAtMillis", cleanDue)
                put("reminderAtMillis", cleanReminder)
                put("reminderEnabled", reminderActive)
                put("categoryId", cleanCategoryId)
                put("subtasks", validatedSubtasks)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }.filterValues { it != null } as Map<String, Any>

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateTask(userId: String, taskId: String, draft: TaskDraft): Result<Unit> {
        var docPath = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireSignedInUid(userId)
            require(isValidFirestoreId(taskId)) { "Invalid task ID" }
            val validatedTitle = validateTitle(draft.title)
            val validatedNotes = validateNotes(draft.notes)
            val validatedSubtasks = serializeSubtasks(draft.subtasks)
            val cleanCategoryId = draft.categoryId?.takeIf { isValidFirestoreId(it) }
            val cleanDue = draft.dueAtMillis?.takeIf { it >= 0L }
            val cleanReminder = if (draft.reminderEnabled && draft.reminderAtMillis != null && draft.reminderAtMillis >= 0L) {
                draft.reminderAtMillis
            } else {
                null
            }
            val reminderActive = draft.reminderEnabled && cleanReminder != null && !draft.completed

            val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
            docPath = docRef.path

            val updates = hashMapOf<String, Any>(
                "title" to validatedTitle,
                "notes" to validatedNotes,
                "completed" to draft.completed,
                "priority" to draft.priority.name,
                "dueAtMillis" to (cleanDue ?: FieldValue.delete()),
                "reminderAtMillis" to (cleanReminder ?: FieldValue.delete()),
                "reminderEnabled" to reminderActive,
                "categoryId" to (cleanCategoryId ?: FieldValue.delete()),
                "subtasks" to validatedSubtasks,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleTaskCompleted(
        userId: String,
        taskId: String,
        completed: Boolean
    ): Result<Unit> {
        var docPath = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireSignedInUid(userId)
            require(isValidFirestoreId(taskId)) { "Invalid task ID" }
            val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
            docPath = docRef.path

            val updates = if (completed) {
                mapOf(
                    "completed" to true,
                    "reminderEnabled" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            } else {
                mapOf(
                    "completed" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSubtasks(
        userId: String,
        taskId: String,
        subtasks: List<SubtaskItem>
    ): Result<Unit> {
        var docPath = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireSignedInUid(userId)
            require(isValidFirestoreId(taskId)) { "Invalid task ID" }
            val serialized = serializeSubtasks(subtasks)
            val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "subtasks" to serialized,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(userId: String, taskId: String): Result<Unit> {
        var docPath = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireSignedInUid(userId)
            require(isValidFirestoreId(taskId)) { "Invalid task ID" }
            val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createCategory(userId: String, name: String, tone: TagTone): Result<String> {
        var docPath = "users/$userId/categories"
        return try {
            val uid = requireSignedInUid(userId)
            val cleanName = validateCategoryName(name)
            val docRef = db.collection("users").document(uid).collection("categories").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "name" to cleanName,
                    "tone" to tone.key,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateCategory(
        userId: String,
        categoryId: String,
        name: String,
        tone: TagTone
    ): Result<Unit> {
        var docPath = "users/$userId/categories/$categoryId"
        return try {
            val uid = requireSignedInUid(userId)
            require(isValidFirestoreId(categoryId)) { "Invalid category ID" }
            val cleanName = validateCategoryName(name)
            val docRef = db.collection("users").document(uid).collection("categories").document(categoryId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "name" to cleanName,
                    "tone" to tone.key,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(
        userId: String,
        categoryId: String,
        affectedTaskIds: List<String>
    ): Result<Unit> {
        var docPath = "users/$userId/categories/$categoryId"
        return try {
            val uid = requireSignedInUid(userId)
            require(isValidFirestoreId(categoryId)) { "Invalid category ID" }
            val userDoc = db.collection("users").document(uid)
            val catRef = userDoc.collection("categories").document(categoryId)
            docPath = catRef.path

            val batch = db.batch()
            batch.delete(catRef)
            affectedTaskIds.filter { isValidFirestoreId(it) }.forEach { tid ->
                val taskRef = userDoc.collection("tasks").document(tid)
                batch.update(
                    taskRef,
                    mapOf(
                        "categoryId" to FieldValue.delete(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun validateTitle(raw: String): String {
        val trimmed = raw.trim()
        require(trimmed.isNotEmpty() && trimmed.length <= MAX_TITLE_LENGTH) {
            "Task title must be between 1 and $MAX_TITLE_LENGTH characters."
        }
        return trimmed
    }

    private fun validateNotes(raw: String): String {
        val trimmed = raw.trim()
        require(trimmed.length <= MAX_NOTES_LENGTH) {
            "Notes cannot exceed $MAX_NOTES_LENGTH characters."
        }
        return trimmed
    }

    private fun validateCategoryName(raw: String): String {
        val trimmed = raw.trim()
        require(trimmed.isNotEmpty() && trimmed.length <= MAX_CATEGORY_LENGTH) {
            "Tag name must be between 1 and $MAX_CATEGORY_LENGTH characters."
        }
        return trimmed
    }

    private fun serializeSubtasks(subtasks: List<SubtaskItem>): List<Map<String, Any>> {
        require(subtasks.size <= MAX_SUBTASKS) {
            "A task can have at most $MAX_SUBTASKS subtasks."
        }
        return subtasks.mapNotNull { item ->
            val cleanTitle = item.title.trim().take(MAX_TITLE_LENGTH)
            if (cleanTitle.isEmpty()) {
                null
            } else {
                val safeId = item.id.takeIf { isValidFirestoreId(it) }
                    ?: "sub_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                mapOf(
                    "id" to safeId,
                    "title" to cleanTitle,
                    "completed" to item.completed
                )
            }
        }
    }

    private fun isValidFirestoreId(id: String): Boolean =
        id.length in 1..128 && ID_REGEX.matches(id)

    companion object {
        const val MAX_TITLE_LENGTH = 120
        const val MAX_NOTES_LENGTH = 1000
        const val MAX_CATEGORY_LENGTH = 30
        const val MAX_SUBTASKS = 30
        private val ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")
    }
}
