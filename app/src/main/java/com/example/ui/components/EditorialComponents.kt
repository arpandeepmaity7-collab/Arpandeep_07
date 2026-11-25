package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryTag
import com.example.data.model.PriorityLevel
import com.example.data.model.SmartFilter
import com.example.data.model.TagTone
import com.example.data.model.TaskItem
import com.example.data.model.priorityLevel
import com.example.data.model.progressFraction
import com.example.data.model.subtaskStats
import com.example.data.model.tagTone
import com.example.ui.theme.FolioMonoMedium
import com.example.ui.theme.FolioMonoSmall
import com.example.ui.theme.IndigoDark
import com.example.ui.theme.IndigoDarkSoft
import com.example.ui.theme.IndigoInk
import com.example.ui.theme.IndigoSoft
import com.example.ui.theme.Ochre
import com.example.ui.theme.OchreDark
import com.example.ui.theme.OchreDarkSoft
import com.example.ui.theme.OchreSoft
import com.example.ui.theme.PlumDark
import com.example.ui.theme.PlumDarkSoft
import com.example.ui.theme.PlumInk
import com.example.ui.theme.PlumSoft
import com.example.ui.theme.Sage
import com.example.ui.theme.SageDark
import com.example.ui.theme.SageDarkSoft
import com.example.ui.theme.SageSoft
import com.example.ui.theme.Terracotta
import com.example.ui.theme.TerracottaDark
import com.example.ui.theme.TerracottaDarkSoft
import com.example.ui.theme.TerracottaSoft
import com.example.ui.workspace.DateFormatting
import com.example.ui.workspace.ProgressSummary
import kotlin.math.roundToInt

data class ToneColors(
    val foreground: Color,
    val background: Color
)

@Composable
fun priorityColors(priority: PriorityLevel): ToneColors {
    val dark = isSystemInDarkTheme()
    return when (priority) {
        PriorityLevel.HIGH -> if (dark) ToneColors(TerracottaDark, TerracottaDarkSoft) else ToneColors(Terracotta, TerracottaSoft)
        PriorityLevel.MEDIUM -> if (dark) ToneColors(OchreDark, OchreDarkSoft) else ToneColors(Ochre, OchreSoft)
        PriorityLevel.LOW -> if (dark) ToneColors(SageDark, SageDarkSoft) else ToneColors(Sage, SageSoft)
    }
}

@Composable
fun tagToneColors(tone: TagTone): ToneColors {
    val dark = isSystemInDarkTheme()
    return when (tone) {
        TagTone.TERRACOTTA -> if (dark) ToneColors(TerracottaDark, TerracottaDarkSoft) else ToneColors(Terracotta, TerracottaSoft)
        TagTone.OCHRE -> if (dark) ToneColors(OchreDark, OchreDarkSoft) else ToneColors(Ochre, OchreSoft)
        TagTone.SAGE -> if (dark) ToneColors(SageDark, SageDarkSoft) else ToneColors(Sage, SageSoft)
        TagTone.INDIGO -> if (dark) ToneColors(IndigoDark, IndigoDarkSoft) else ToneColors(IndigoInk, IndigoSoft)
        TagTone.PLUM -> if (dark) ToneColors(PlumDark, PlumDarkSoft) else ToneColors(PlumInk, PlumSoft)
        TagTone.CHARCOAL -> ToneColors(
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun EditorialProgressBanner(
    summary: ProgressSummary,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = summary.completionRatio,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "overall_progress"
    )
    val percent = (animatedProgress * 100f).roundToInt()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (summary.totalCount == 0) {
                            "A clean page awaits"
                        } else {
                            "${summary.completedCount} of ${summary.totalCount} completed"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            summary.totalCount == 0 -> "Capture your first task with a due date and priority."
                            summary.overdueCount > 0 -> "${summary.overdueCount} overdue • ${summary.dueTodayCount} due today"
                            summary.dueTodayCount > 0 -> "${summary.dueTodayCount} due today • ${summary.activeCount} active"
                            else -> "${summary.activeCount} active tasks remaining"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (summary.overdueCount > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = "$percent%",
                        style = FolioMonoMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (percent == 100 && summary.totalCount > 0) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.secondary
                            }
                        )
                )
            }
        }
    }
}

@Composable
fun SmartFilterStrip(
    selectedFilter: SmartFilter,
    filterCounts: Map<SmartFilter, Int>,
    onSelectFilter: (SmartFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(SmartFilter.entries) { filter ->
            val selected = filter == selectedFilter
            val count = filterCounts[filter] ?: 0
            val isOverdueAlert = filter == SmartFilter.OVERDUE && count > 0 && !selected

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when {
                    selected -> MaterialTheme.colorScheme.primary
                    isOverdueAlert -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    1.dp,
                    when {
                        selected -> MaterialTheme.colorScheme.primary
                        isOverdueAlert -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectFilter(filter) }
                    .testTag("filter_chip_${filter.name.lowercase()}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            selected -> MaterialTheme.colorScheme.onPrimary
                            isOverdueAlert -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = "$count",
                        style = FolioMonoSmall,
                        color = when {
                            selected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            isOverdueAlert -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryFilterStrip(
    categories: List<CategoryTag>,
    selectedCategoryId: String?,
    onSelectCategory: (String?) -> Unit,
    onManageTags: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onManageTags() }
                    .testTag("manage_tags_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Manage project tags",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (categories.isEmpty()) "Add Tag" else "Tags",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(categories, key = { it.id }) { tag ->
            val selected = tag.id == selectedCategoryId
            val colors = tagToneColors(tag.tagTone())

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selected) colors.foreground else colors.background,
                border = BorderStroke(1.dp, colors.foreground.copy(alpha = if (selected) 1f else 0.35f)),
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectCategory(tag.id) }
                    .testTag("category_chip_${tag.id}")
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
                            .background(if (selected) MaterialTheme.colorScheme.surface else colors.foreground)
                    )
                    Text(
                        text = tag.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.surface else colors.foreground
                    )
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: TaskItem,
    category: CategoryTag?,
    isSelected: Boolean,
    onToggleComplete: () -> Unit,
    onToggleSubtask: (String) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priority = task.priorityLevel()
    val pColors = priorityColors(priority)
    val isOverdue = DateFormatting.isOverdue(task.dueAtMillis, task.completed)
    val isDueToday = DateFormatting.isDueToday(task.dueAtMillis)
    val (subDone, subTotal) = task.subtaskStats()
    val progress = task.progressFraction()
    val animatedSubProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "task_sub_progress"
    )

    var subtasksExpanded by remember(task.id) { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("task_item_card_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(start = 8.dp, end = 14.dp, top = 10.dp, bottom = 12.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 48dp minimum touch target custom ink checkbox
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .clickable { onToggleComplete() }
                        .testTag("task_checkbox_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                if (task.completed) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .border(
                                width = 1.6.dp,
                                color = if (task.completed) {
                                    MaterialTheme.colorScheme.primary
                                } else if (isOverdue) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (task.completed) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Completed",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (task.completed) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = pColors.background
                        ) {
                            Text(
                                text = priority.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = pColors.foreground,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (task.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Metadata badges row: Due date, Reminder bell, Category tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        task.dueAtMillis?.let { dueMillis ->
                            val badgeText = DateFormatting.formatDueBadge(dueMillis)
                            val highlightDue = !task.completed && (isOverdue || isDueToday)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (highlightDue) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarToday,
                                        contentDescription = null,
                                        tint = if (highlightDue) {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (isOverdue) "Overdue · $badgeText" else badgeText,
                                        style = FolioMonoSmall,
                                        color = if (highlightDue) {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }

                        if (task.reminderEnabled && !task.completed && task.reminderAtMillis != null) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = "Reminder scheduled",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        if (category != null) {
                            val tColors = tagToneColors(category.tagTone())
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = tColors.background
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Label,
                                        contentDescription = null,
                                        tint = tColors.foreground,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = tColors.foreground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Per-task Subtasks Progress Bar + Expandable Inline Checklist
                    if (subTotal > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { subtasksExpanded = !subtasksExpanded }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedSubProgress.coerceIn(0f, 1f))
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            if (subDone == subTotal) {
                                                MaterialTheme.colorScheme.tertiary
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            }
                                        )
                                )
                            }
                            Text(
                                text = "$subDone/$subTotal",
                                style = FolioMonoSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = if (subtasksExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = if (subtasksExpanded) "Collapse subtasks" else "Expand subtasks",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(visible = subtasksExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(6.dp))
                                task.subtasks.forEach { sub ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 42.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onToggleSubtask(sub.id) }
                                            .padding(horizontal = 4.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (sub.completed) MaterialTheme.colorScheme.tertiary else Color.Transparent
                                                )
                                                .border(
                                                    1.3.dp,
                                                    if (sub.completed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                                                    RoundedCornerShape(4.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (sub.completed) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onTertiary,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = sub.title,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                textDecoration = if (sub.completed) TextDecoration.LineThrough else TextDecoration.None
                                            ),
                                            color = if (sub.completed) {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
