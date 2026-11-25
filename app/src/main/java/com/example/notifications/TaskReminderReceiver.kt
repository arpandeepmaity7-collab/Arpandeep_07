package com.example.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(TaskReminderScheduler.EXTRA_TASK_ID) ?: return
        val title = intent.getStringExtra(TaskReminderScheduler.EXTRA_TASK_TITLE) ?: return
        val priorityLabel = intent.getStringExtra(TaskReminderScheduler.EXTRA_PRIORITY_LABEL) ?: "Medium"

        TaskReminderScheduler.ensureNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(TaskReminderScheduler.EXTRA_TASK_ID, taskId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, TaskReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_folio)
            .setContentTitle(title)
            .setContentText("$priorityLabel priority reminder • Tap to review in Folio Tasks")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(taskId.hashCode(), notification)
    }
}
