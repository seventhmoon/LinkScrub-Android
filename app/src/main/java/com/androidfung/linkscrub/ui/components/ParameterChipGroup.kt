package com.androidfung.linkscrub.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.androidfung.linkscrub.ui.ParameterChipItem
import com.androidfung.linkscrub.ui.theme.LinkScrubTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParameterChipGroup(
    parameters: List<ParameterChipItem>,
    onToggleParameter: (String) -> Unit,
    onSelectAllTracking: () -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with stats and bulk action buttons
            val trackingCount = parameters.count { it.isTracking }
            val markedCount = parameters.count { it.isMarkedForRemoval }

            val haptic = LocalHapticFeedback.current

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Query Parameters (${parameters.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$markedCount marked for scrub ($trackingCount tracking)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelectAllTracking()
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = "Scrub All",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Scrub All",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onResetAll()
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Keep All",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Keep All",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (parameters.isEmpty()) {
                Text(
                    text = "No query parameters found in this URL. The URL is clean!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                // Parameter chips flow
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    parameters.forEach { param ->
                        ParameterChip(
                            item = param,
                            onToggle = { onToggleParameter(param.key) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ParameterChip(
    item: ParameterChipItem,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRemoved = item.isMarkedForRemoval
    val isTracking = item.isTracking
    val haptic = LocalHapticFeedback.current

    FilterChip(
        selected = isRemoved,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onToggle()
        },
        label = {
            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                Text(
                    text = item.key,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.value.isNotBlank()) {
                    Text(
                        text = if (item.value.length > 18) item.value.take(15) + "…" else item.value,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        leadingIcon = {
            if (isRemoved) {
                Icon(
                    imageVector = Icons.Rounded.Block,
                    contentDescription = "Marked for scrub",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            } else if (isTracking) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = "Tracking parameter",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Safe parameter",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun ParameterChipGroupPreview() {
    LinkScrubTheme {
        ParameterChipGroup(
            parameters = listOf(
                ParameterChipItem("utm_source", "newsletter", isTracking = true, isMarkedForRemoval = true),
                ParameterChipItem("utm_medium", "email", isTracking = true, isMarkedForRemoval = true),
                ParameterChipItem("fbclid", "IwAR123abc", isTracking = true, isMarkedForRemoval = true),
                ParameterChipItem("id", "89342", isTracking = false, isMarkedForRemoval = false),
                ParameterChipItem("sort", "desc", isTracking = false, isMarkedForRemoval = false)
            ),
            onToggleParameter = {},
            onSelectAllTracking = {},
            onResetAll = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
