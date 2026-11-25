package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CategoryTag
import com.example.data.model.TagTone
import com.example.data.model.tagTone
import com.example.data.repository.FirestoreTaskRepository

@Composable
fun TagManagerDialog(
    categories: List<CategoryTag>,
    onDismiss: () -> Unit,
    onCreateTag: (String, TagTone) -> Unit,
    onUpdateTag: (String, String, TagTone) -> Unit,
    onDeleteTag: (String) -> Unit
) {
    var editingTag by remember { mutableStateOf<CategoryTag?>(null) }
    var nameInput by remember { mutableStateOf("") }
    var selectedTone by remember { mutableStateOf(TagTone.TERRACOTTA) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingTag == null) stringResource(R.string.manage_tags) else "Edit Tag",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        if (it.length <= FirestoreTaskRepository.MAX_CATEGORY_LENGTH) {
                            nameInput = it
                        }
                    },
                    label = { Text("Tag name") },
                    placeholder = { Text("e.g., Studio, Personal, Errands") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tag_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Ink Tone",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TagTone.entries.forEach { tone ->
                        val colors = tagToneColors(tone)
                        val selected = selectedTone == tone
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.foreground)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else colors.background,
                                    shape = CircleShape
                                )
                                .clickable { selectedTone = tone }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val clean = nameInput.trim()
                            if (clean.isNotEmpty()) {
                                val current = editingTag
                                if (current == null) {
                                    onCreateTag(clean, selectedTone)
                                } else {
                                    onUpdateTag(current.id, clean, selectedTone)
                                    editingTag = null
                                }
                                nameInput = ""
                            }
                        },
                        enabled = nameInput.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("save_tag_button")
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (editingTag == null) "Add Tag" else "Update")
                    }
                    if (editingTag != null) {
                        OutlinedButton(
                            onClick = {
                                editingTag = null
                                nameInput = ""
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel")
                        }
                    }
                }

                if (categories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    categories.forEach { tag ->
                        val tColors = tagToneColors(tag.tagTone())
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = tColors.background
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(tColors.foreground)
                                    )
                                    Text(
                                        text = tag.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = tColors.foreground
                                    )
                                }
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        editingTag = tag
                                        nameInput = tag.name
                                        selectedTone = tag.tagTone()
                                    }
                                ) {
                                    Icon(
                                        Icons.Outlined.Edit,
                                        contentDescription = "Edit tag ${tag.name}",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(onClick = { onDeleteTag(tag.id) }) {
                                    Icon(
                                        Icons.Outlined.DeleteOutline,
                                        contentDescription = "Delete tag ${tag.name}",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun AccountDialog(
    displayName: String?,
    email: String?,
    onDismiss: () -> Unit,
    onSignOut: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notebook Account", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = displayName?.takeIf { it.isNotBlank() } ?: "Google Account",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!email.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tasks and tags are synced in real time to your private cloud workspace.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier.testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.sign_out))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
