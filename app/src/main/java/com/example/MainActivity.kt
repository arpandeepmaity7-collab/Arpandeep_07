package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AuthScreen
import com.example.auth.authStateFlow
import com.example.data.repository.FirestoreTaskRepository
import com.example.notifications.TaskReminderScheduler
import com.example.ui.theme.FolioTasksTheme
import com.example.ui.workspace.FolioWorkspaceScreen
import com.example.ui.workspace.TaskWorkspaceViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    private var launchedTaskId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        launchedTaskId = intent?.getStringExtra(TaskReminderScheduler.EXTRA_TASK_ID)

        setContent {
            FolioTasksTheme {
                AppNavigation(
                    initialTaskId = launchedTaskId,
                    onConsumeInitialTaskId = { launchedTaskId = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val taskId = intent.getStringExtra(TaskReminderScheduler.EXTRA_TASK_ID)
        if (!taskId.isNullOrBlank()) {
            launchedTaskId = taskId
        }
    }
}

@Composable
fun AppNavigation(
    initialTaskId: String?,
    onConsumeInitialTaskId: () -> Unit,
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = { /* authStateFlow updates automatically */ })
    } else {
        val uid = user.uid
        key(uid) {
            val viewModel: TaskWorkspaceViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        val repository = FirestoreTaskRepository(db)
                        val scheduler = TaskReminderScheduler(app)
                        TaskWorkspaceViewModel(
                            repository = repository,
                            reminderScheduler = scheduler,
                            currentUserId = uid,
                            initialTaskId = initialTaskId
                        )
                    }
                }
            )
            LaunchedEffect(initialTaskId) {
                if (initialTaskId != null) {
                    viewModel.selectTask(initialTaskId)
                    onConsumeInitialTaskId()
                }
            }
            FolioWorkspaceScreen(viewModel = viewModel)
        }
    }
}
