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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.androidfung.linkscrub.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsBottomSheet(
    isQuickCleanEnabled: Boolean,
    unshortenRedirects: Boolean,
    fetchOpenGraphPreview: Boolean,
    customBlocklist: Set<String>,
    customAllowlist: Set<String>,
    onQuickCleanChange: (Boolean) -> Unit,
    onUnshortenChange: (Boolean) -> Unit,
    onFetchOgPreviewChange: (Boolean) -> Unit,
    onAddBlockedParam: (String) -> Unit,
    onRemoveBlockedParam: (String) -> Unit,
    onAddAllowedParam: (String) -> Unit,
    onRemoveAllowedParam: (String) -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var blockedInput by remember { mutableStateOf("") }
    var allowedInput by remember { mutableStateOf("") }

    val submitBlocked = {
        blockedInput.split(",", " ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { onAddBlockedParam(it) }
        blockedInput = ""
    }

    val submitAllowed = {
        allowedInput.split(",", " ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { onAddAllowedParam(it) }
        allowedInput = ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.settings_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Section 1: Sanitization Behavior
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_section_behavior),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    SettingsToggleRow(
                        title = stringResource(R.string.settings_quick_clean_title),
                        subtitle = stringResource(R.string.settings_quick_clean_subtitle),
                        checked = isQuickCleanEnabled,
                        onCheckedChange = onQuickCleanChange
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsToggleRow(
                        title = stringResource(R.string.settings_unshorten_title),
                        subtitle = stringResource(R.string.settings_unshorten_subtitle),
                        checked = unshortenRedirects,
                        onCheckedChange = onUnshortenChange
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    SettingsToggleRow(
                        title = stringResource(R.string.settings_og_preview_title),
                        subtitle = stringResource(R.string.settings_og_preview_subtitle),
                        checked = fetchOpenGraphPreview,
                        onCheckedChange = onFetchOgPreviewChange
                    )
                }
            }

            // Section 2: Custom Blocked Parameters
            CustomRuleCard(
                title = stringResource(R.string.settings_section_blocklist),
                subtitle = stringResource(R.string.settings_blocklist_subtitle),
                placeholder = stringResource(R.string.settings_blocklist_placeholder),
                inputValue = blockedInput,
                onInputValueChange = { blockedInput = it },
                onSubmit = submitBlocked,
                items = customBlocklist.sorted(),
                onRemoveItem = onRemoveBlockedParam,
                isBlocklist = true
            )

            // Section 3: Custom Allowed Parameters
            CustomRuleCard(
                title = stringResource(R.string.settings_section_allowlist),
                subtitle = stringResource(R.string.settings_allowlist_subtitle),
                placeholder = stringResource(R.string.settings_allowlist_placeholder),
                inputValue = allowedInput,
                onInputValueChange = { allowedInput = it },
                onSubmit = submitAllowed,
                items = customAllowlist.sorted(),
                onRemoveItem = onRemoveAllowedParam,
                isBlocklist = false
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomRuleCard(
    title: String,
    subtitle: String,
    placeholder: String,
    inputValue: String,
    onInputValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    items: List<String>,
    onRemoveItem: (String) -> Unit,
    isBlocklist: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isBlocklist) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = onInputValueChange,
                    placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (inputValue.isNotBlank()) onSubmit() }),
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = onSubmit,
                    enabled = inputValue.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.settings_action_add))
                }
            }

            if (items.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_empty_rules),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items.forEach { param ->
                        InputChip(
                            selected = true,
                            onClick = { onRemoveItem(param) },
                            label = {
                                Text(
                                    text = param,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isBlocklist) Icons.Rounded.Block else Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.cd_delete),
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = if (isBlocklist) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                },
                                selectedLabelColor = if (isBlocklist) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}
