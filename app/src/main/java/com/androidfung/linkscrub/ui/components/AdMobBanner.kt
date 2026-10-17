package com.androidfung.linkscrub.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.androidfung.linkscrub.ui.theme.LinkScrubTheme
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import android.content.pm.ApplicationInfo
import com.google.android.gms.ads.LoadAdError

const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
const val PROD_BANNER_AD_UNIT_ID = "ca-app-pub-9890667024752973/7297786455"

/**
 * Google AdMob Adaptive Banner Ad Composable.
 *
 * Wraps an [AdView] using [AndroidView], dynamically computes an anchored adaptive banner ad size
 * based on the actual available width measured via [BoxWithConstraints] rather than full device width.
 * Automatically uses Google's official test ad unit ID when running in debug builds to prevent policy violations.
 *
 * @param adUnitId The AdMob banner ad unit ID (defaults to test ID in debug builds, production ID otherwise).
 * @param modifier The modifier for layout customisation.
 */
@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier,
    adUnitId: String? = null
) {
    val isInspectionMode = LocalInspectionMode.current

    // In Compose Previews / Inspection mode, render a clean placeholder
    if (isInspectionMode) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AdMob Adaptive Banner Preview",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    var adViewInstance by remember { mutableStateOf<AdView?>(null) }
    var isAdLoaded by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            adViewInstance?.destroy()
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val availableWidthDp = maxWidth.value.toInt()
        if (availableWidthDp > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        modifier = Modifier.wrapContentHeight(),
                        factory = { ctx ->
                            val isDebug = (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                            val targetAdUnitId = adUnitId ?: if (isDebug) TEST_BANNER_AD_UNIT_ID else PROD_BANNER_AD_UNIT_ID

                            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                                ctx,
                                availableWidthDp
                            )
                            val adHeightPx = adSize.getHeightInPixels(ctx)

                            AdView(ctx).apply {
                                this.adUnitId = targetAdUnitId
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                                    if (adHeightPx > 0) adHeightPx else android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                                setAdSize(adSize)

                                adListener = object : AdListener() {
                                    override fun onAdLoaded() {
                                        super.onAdLoaded()
                                        isAdLoaded = true
                                    }

                                    override fun onAdImpression() {
                                        super.onAdImpression()
                                        com.androidfung.linkscrub.util.AnalyticsHelper.logAdImpression()
                                    }

                                    override fun onAdFailedToLoad(error: LoadAdError) {
                                        super.onAdFailedToLoad(error)
                                        isAdLoaded = false
                                    }
                                }

                                loadAd(AdRequest.Builder().build())
                                adViewInstance = this
                            }
                        },
                        update = {
                            // AdView handles updates internally
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdMobBannerPreview() {
    LinkScrubTheme {
        AdMobBanner(
            modifier = Modifier.padding(16.dp)
        )
    }
}

