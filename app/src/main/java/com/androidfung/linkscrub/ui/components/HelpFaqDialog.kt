package com.androidfung.linkscrub.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SheetState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpFaqBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("FAQ", "How It Works", "About")

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Help & Information",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Guides, FAQs, and privacy principles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content per tab
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                when (selectedTab) {
                    0 -> FaqSection()
                    1 -> HowItWorksSection()
                    2 -> AboutSection()
                }
            }
        }
    }
}

@Composable
private fun FaqSection() {
    val expandedMap = remember { mutableStateMapOf<Int, Boolean>() }

    val faqs = remember {
        listOf(
            FaqItem(
                question = "What does LinkScrub do?",
                answer = "LinkScrub cleans web addresses (URLs) by stripping tracking parameters (such as utm_*, fbclid, gclid, and session markers). It also unshortens short links (bit.ly, t.co) so you can see where they really lead before sharing or visiting."
            ),
            FaqItem(
                question = "Does LinkScrub store or upload my links?",
                answer = "No. LinkScrub is 100% on-device and privacy-first. Your links, parameters, and browsing history are never transmitted to external cloud servers. All link analysis and history are kept strictly on your phone."
            ),
            FaqItem(
                question = "Will scrubbing break important features like YouTube video timestamps?",
                answer = "No. LinkScrub features domain-aware intelligence that preserves essential parameters (such as 't=...' start timestamps on YouTube videos and core product paths on Amazon) while stripping tracking garbage (like 'si=' or referral tags)."
            ),
            FaqItem(
                question = "How do I use LinkScrub from other apps?",
                answer = "Whenever you want to clean a link in Twitter, WhatsApp, Chrome, or Reddit, tap 'Share' in that app and choose 'LinkScrub'. LinkScrub will automatically receive, scrub, and allow you to copy or re-share the clean URL immediately."
            ),
            FaqItem(
                question = "What is Quick-Clean Mode?",
                answer = "Quick-Clean Mode allows you to paste a link and have it immediately sanitized and copied to your clipboard with a single tap, bypassing intermediate review steps for speedy sharing."
            ),
            FaqItem(
                question = "How does unshortening work?",
                answer = "Short URLs (e.g. bit.ly, t.co) redirect through one or more hops to their destination. LinkScrub follows these HTTP redirect headers on-device with built-in cyclic loop detection to reveal the genuine destination safely."
            )
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        faqs.forEachIndexed { index, item ->
            val isExpanded = expandedMap[index] ?: (index == 0)
            FaqCard(
                faq = item,
                isExpanded = isExpanded,
                onToggle = { expandedMap[index] = !isExpanded }
            )
        }
    }
}

private data class FaqItem(val question: String, val answer: String)

@Composable
private fun FaqCard(
    faq: FaqItem,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.QuestionAnswer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = faq.question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = faq.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HowItWorksSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HowItWorksStepCard(
            stepNumber = "1",
            title = "Input & Detection",
            description = "Paste a URL, scan a QR code, or share directly to LinkScrub from another app. LinkScrub automatically parses links even when embedded in conversational text blocks.",
            icon = Icons.Rounded.CleaningServices
        )

        HowItWorksStepCard(
            stepNumber = "2",
            title = "Inspect & Parameter Stripping",
            description = "LinkScrub inspects query tokens and categorizes them into known tracking markers (UTM, Facebook fbclid, Google Ads gclid, TikTok telemetry) and legitimate parameters. You can toggle any parameter on or off.",
            icon = Icons.Rounded.Shield
        )

        HowItWorksStepCard(
            stepNumber = "3",
            title = "Unshortening & Open Graph Preview",
            description = "If the URL is a shortlink, redirect headers are resolved with cyclic loop protection. Rich metadata (title, preview image, description) is fetched so you can verify the target safely before sharing.",
            icon = Icons.Rounded.Lock
        )
    }
}

@Composable
private fun HowItWorksStepCard(
    stepNumber: String,
    title: String,
    description: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🧼 LinkScrub",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Clean, Private, and Intelligent URL Sanitizer for Android. Free and open-source under the GNU General Public License v3.0 (GPLv3).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Action links
        ActionLinkCard(
            title = "View on Google Play",
            subtitle = "Rate and review LinkScrub on Play Store",
            icon = Icons.AutoMirrored.Rounded.OpenInNew,
            onClick = {
                openWebUrl(
                    context,
                    "https://play.google.com/store/apps/details?id=com.androidfung.linkscrub"
                )
            }
        )

        ActionLinkCard(
            title = "Source Code on GitHub",
            subtitle = "Contribute, report issues, or view code",
            icon = Icons.Rounded.Info,
            onClick = {
                openWebUrl(
                    context,
                    "https://github.com/seventhmoon/LinkScrub-Android"
                )
            }
        )

        ActionLinkCard(
            title = "Privacy Policy",
            subtitle = "Read our 100% on-device privacy guarantee",
            icon = Icons.Rounded.Policy,
            onClick = {
                openWebUrl(
                    context,
                    "https://github.com/seventhmoon/LinkScrub-Android/blob/main/PRIVACY_POLICY.md"
                )
            }
        )
    }
}

@Composable
private fun ActionLinkCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
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
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun openWebUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}
