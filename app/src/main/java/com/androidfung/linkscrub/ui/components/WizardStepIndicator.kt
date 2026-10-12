package com.androidfung.linkscrub.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Preview
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.androidfung.linkscrub.R
import com.androidfung.linkscrub.ui.WizardStep
import com.androidfung.linkscrub.ui.theme.LinkScrubTheme

@Composable
fun WizardStepIndicator(
    currentStep: WizardStep,
    onStepClick: (WizardStep) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            WizardStep.entries.forEachIndexed { index, step ->
                val isCompleted = currentStep.stepNumber > step.stepNumber
                val isCurrent = currentStep == step

                StepItem(
                    step = step,
                    isCurrent = isCurrent,
                    isCompleted = isCompleted,
                    onClick = {
                        // Allow clicking back to completed steps or current
                        if (isCompleted || isCurrent) {
                            onStepClick(step)
                        }
                    }
                )

                if (index < WizardStep.entries.size - 1) {
                    val connectorColor by animateColorAsState(
                        targetValue = if (currentStep.stepNumber > index + 1) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        animationSpec = tween(durationMillis = 300),
                        label = "connectorColor"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(connectorColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepItem(
    step: WizardStep,
    isCurrent: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    val stepTitle = stringResource(step.titleRes)
    val bgColor by animateColorAsState(
        targetValue = when {
            isCurrent -> MaterialTheme.colorScheme.primary
            isCompleted -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(300),
        label = "stepBgColor"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isCurrent -> MaterialTheme.colorScheme.onPrimary
            isCompleted -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(300),
        label = "stepContentColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(enabled = isCompleted || isCurrent, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = stringResource(R.string.wizard_step_completed, stepTitle),
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                val icon = when (step) {
                    WizardStep.INPUT -> Icons.Rounded.Edit
                    WizardStep.INSPECT -> Icons.Rounded.Tune
                    WizardStep.PREVIEW -> Icons.Rounded.Preview
                }
                Icon(
                    imageVector = icon,
                    contentDescription = stepTitle,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.wizard_step_number, step.stepNumber),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WizardStepIndicatorPreview() {
    LinkScrubTheme {
        WizardStepIndicator(
            currentStep = WizardStep.INSPECT,
            onStepClick = {}
        )
    }
}
