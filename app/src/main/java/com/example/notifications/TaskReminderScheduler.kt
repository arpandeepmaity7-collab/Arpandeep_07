package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.R
import com.example.data.model.TaskItem
import com.example.data.model.priorityLevel
import org.json.JSONArray
import org.json.JSONObject

data class ScheduledReminderRecord(
    val taskId: String,
    val title: String,
    val priorityLabel: String,
    val reminderAtMillis: Long,
    val dueAtMillis: Long?
)

class TaskReminderScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        ensureNotificationChannel(appContext)
    }

    fun syncTaskReminders(tasks: List<TaskItem>) {
        val now = System.currentTimeMillis()
        val previouslyScheduledIds = loadStoredRecords().map { it.taskId }.toSet()

        val activeRecords = tasks.mapNotNull { task ->
            val triggerAt = task.reminderAtMillis
            if (!task.completed && task.reminderEnabled && triggerAt != null && triggerAt > now + 1000L && task.id.isNotBlank()) {
                ScheduledReminderRecord(
                    taskId = task.id,
                    title = task.title,
                    priorityLabel = task.priorityLevel().label,
                    reminderAtMillis = triggerAt,
                    dueAtMillis = task.dueAtMillis
                )
            } else {
                null
            }
        }

        val activeIds = activeRecords.map { it.taskId }.toSet()
        previouslyScheduledIds.filterNot { it in activeIds }.forEach { removedId ->
            cancelAlarm(removedId)
        }

        activeRecords.forEach { record ->
            scheduleAlarm(record)
        }
        saveRecords(activeRecords)
    }

    fun reschedulePersistedReminders() {
        val now = System.currentTimeMillis()
        val valid = loadStoredRecords().filter { it.reminderAtMillis > now + 1000L }
        valid.forEach { scheduleAlarm(it) }
        saveRecords(valid)
    }

    fun clearAllReminders() {
        loadStoredRecords().forEach { cancelAlarm(it.taskId) }
        prefs.edit().remove(KEY_REMINDERS).apply()
    }

    private fun scheduleAlarm(record: ScheduledReminderRecord) {
        val pendingIntent = buildReminderPendingIntent(appContext, record)
        val canUseExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        try {
            if (canUseExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    record.reminderAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    record.reminderAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                record.reminderAtMillis,
                pendingIntent
            )
        }
    }

    private fun cancelAlarm(taskId: String) {
        val intent = Intent(appContext, TaskReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun saveRecords(records: List<ScheduledReminderRecord>) {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject().apply {
                    put("taskId", record.taskId)
                    put("title", record.title)
                    put("priorityLabel", record.priorityLabel)
                    put("reminderAtMillis", record.reminderAtMillis)
                    if (record.dueAtMillis != null) {
                        put("dueAtMillis", record.dueAtMillis)
                    }
                }
            )
        }
        prefs.edit().putString(KEY_REMINDERS, array.toString()).apply()
    }

    private fun loadStoredRecords(): List<ScheduledReminderRecord> {
        val raw = prefs.getString(KEY_REMINDERS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val taskId = obj.optString("taskId", "")
                    val title = obj.optString("title", "")
                    if (taskId.isNotBlank() && title.isNotBlank()) {
                        add(
                            ScheduledReminderRecord(
                                taskId = taskId,
                                title = title,
                                priorityLabel = obj.optString("priorityLabel", "Medium"),
                                reminderAtMillis = obj.optLong("reminderAtMillis", 0L),
                                dueAtMillis = if (obj.has("dueAtMillis")) obj.optLong("dueAtMillis") else null
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        const val CHANNEL_ID = "folio_task_reminders"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_PRIORITY_LABEL = "extra_priority_label"
        const val EXTRA_DUE_MILLIS = "extra_due_millis"
        private const val PREFS_NAME = "folio_reminders_prefs"
        private const val KEY_REMINDERS = "scheduled_reminders_json"

        fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val existing = manager.getNotificationChannel(CHANNEL_ID)
                if (existing == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.reminder_channel_name),
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = context.getString(R.string.reminder_channel_description)
                    }
                    manager.createNotificationChannel(channel)
                }
            }
        }

        fun buildReminderPendingIntent(
            context: Context,
            record: ScheduledReminderRecord
        ): PendingIntent {
            val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                putExtra(EXTRA_TASK_ID, record.taskId)
                putExtra(EXTRA_TASK_TITLE, record.title)
                putExtra(EXTRA_PRIORITY_LABEL, record.priorityLabel)
                if (record.dueAtMillis != null) {
                    putExtra(EXTRA_DUE_MILLIS, record.dueAtMillis)
                }
            }
            return PendingIntent.getBroadcast(
                context,
                record.taskId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
