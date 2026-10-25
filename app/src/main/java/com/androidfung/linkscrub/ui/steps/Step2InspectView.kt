package com.androidfung.linkscrub.ui.steps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.androidfung.linkscrub.R
import com.androidfung.linkscrub.ui.ParameterChipItem
import com.androidfung.linkscrub.ui.components.ParameterChipGroup

/**
 * Encapsulates user interactions for Step 2.
 */
data class Step2Actions(
    val onToggleParam: (String) -> Unit,
    val onScrubAllTracking: () -> Unit,
    val onKeepAll: () -> Unit,
    val onBack: () -> Unit,
    val onProceedToPreview: () -> Unit
)

/**
 * Step 2: Parameter inspection, unshortening info, and interactive chip removal
 */
@Composable
fun Step2InspectView(
    inputUrl: String,
    unshortenedUrl: String?,
    isRedirected: Boolean,
    redirectChain: List<String>,
    isUnshortening: Boolean,
    parameters: List<ParameterChipItem>,
    cleanedUrlCandidate: String,
    actions: Step2Actions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Redirect & Unshortening notification card
        if (isUnshortening) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.inspect_unshortening),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else if (isRedirected && !unshortenedUrl.isNullOrBlank()) {
            var isChainExpanded by remember { mutableStateOf(false) }
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = redirectChain.isNotEmpty()) {
                        isChainExpanded = !isChainExpanded
                    }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Route,
                                contentDescription = stringResource(R.string.cd_redirect_traced),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.inspect_redirect_resolved, redirectChain.size),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        if (redirectChain.isNotEmpty()) {
                            Icon(
                                imageVector = if (isChainExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (isChainExpanded) stringResource(R.string.cd_collapse) else stringResource(R.string.cd_expand),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.inspect_destination, unshortenedUrl),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = if (isChainExpanded) 4 else 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    AnimatedVisibility(visible = isChainExpanded && redirectChain.isNotEmpty()) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            redirectChain.forEachIndexed { index, hopUrl ->
                                Text(
                                    text = stringResource(R.string.inspect_redirect_hop_item, index + 1, hopUrl),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Parameter Chip Group
        ParameterChipGroup(
            parameters = parameters,
            onToggleParameter = actions.onToggleParam,
            onSelectAllTracking = actions.onScrubAllTracking,
            onResetAll = actions.onKeepAll
        )

        // Real-time Preview of Candidate Clean URL
        val effectiveSource = unshortenedUrl ?: inputUrl
        val displayCandidate = cleanedUrlCandidate.ifBlank { effectiveSource }
        val removedCount = parameters.count { it.isMarkedForRemoval }
        val charsSaved = (effectiveSource.length - displayCandidate.length).coerceAtLeast(0)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.inspect_candidate_preview_title),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (removedCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.inspect_savings_badge, removedCount, charsSaved),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = displayCandidate,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Navigation controls (Back & Forward)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = actions.onBack,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.action_back)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.action_back),
                    maxLines = 1,
                    softWrap = false
                )
            }

            Button(
                onClick = actions.onProceedToPreview,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.weight(2f).height(50.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_confirm_preview),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = stringResource(R.string.cd_preview)
                )
            }
        }
    }
}
