package com.androidfung.linkscrub.ui.components

import android.content.pm.ApplicationInfo
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.androidfung.linkscrub.R
import com.androidfung.linkscrub.ui.theme.LinkScrubTheme
import com.androidfung.linkscrub.util.AnalyticsHelper
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

// Google Official Test Ad Unit ID for Native Ads
const val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
// Production Native Ad Unit ID for LinkScrub
const val PROD_NATIVE_AD_UNIT_ID = "ca-app-pub-9890667024752973/4211244060"

/**
 * AdMob Native Ad Card adhering to Google AdMob Best Practices & Policy:
 * 1. Unambiguous "Ad" Attribution badge (minimum 15x15dp, high contrast)
 * 2. Automatic AdChoices overlay placement with adequate padding/visibility
 * 3. Proper asset registration with [NativeAdView] to ensure accurate impressions and click handling
 * 4. Proper lifecycle management: calls [NativeAd.destroy] on disposal to prevent memory leaks
 * 5. Uses official Google test ad unit in debug builds and production ad unit in release builds
 * 6. Visual style harmonizes with Material 3 elevated card container
 */
@Composable
fun AdMobNativeCard(
    modifier: Modifier = Modifier,
    adUnitId: String? = null
) {
    val isInspectionMode = LocalInspectionMode.current

    if (isInspectionMode) {
        NativeAdPlaceholder(modifier)
        return
    }

    val context = LocalContext.current
    var loadedNativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var isAdFailed by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val targetAdUnitId = adUnitId ?: if (isDebug) TEST_NATIVE_AD_UNIT_ID else PROD_NATIVE_AD_UNIT_ID

        val adLoader = AdLoader.Builder(context, targetAdUnitId)
            .forNativeAd { nativeAd ->
                loadedNativeAd?.destroy()
                loadedNativeAd = nativeAd
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    isAdFailed = true
                }

                override fun onAdImpression() {
                    AnalyticsHelper.logAdImpression()
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

        onDispose {
            loadedNativeAd?.destroy()
            loadedNativeAd = null
        }
    }

    val nativeAd = loadedNativeAd
    if (nativeAd != null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    val frameLayout = FrameLayout(ctx)
                    val inflater = LayoutInflater.from(ctx)
                    val view = inflater.inflate(R.layout.admob_native_card_layout, frameLayout, false) as NativeAdView
                    populateNativeAdView(nativeAd, view)
                    view
                },
                update = { view ->
                    populateNativeAdView(nativeAd, view)
                }
            )
        }
    }
}

/**
 * Binds and registers Native Ad assets with [NativeAdView] according to Google GMA guidelines.
 */
private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
    // 1. Headline
    val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
    headlineView.text = nativeAd.headline
    headlineView.visibility = if (!nativeAd.headline.isNullOrBlank()) View.VISIBLE else View.INVISIBLE
    adView.headlineView = headlineView

    // 2. App Icon
    val iconView = adView.findViewById<ImageView>(R.id.ad_app_icon)
    val iconAsset = nativeAd.icon
    if (iconAsset?.drawable != null) {
        iconView.setImageDrawable(iconAsset.drawable)
        iconView.visibility = View.VISIBLE
    } else {
        iconView.visibility = View.GONE
    }
    adView.iconView = iconView

    // 3. Body text
    val bodyView = adView.findViewById<TextView>(R.id.ad_body)
    val bodyText = nativeAd.body
    if (!bodyText.isNullOrBlank()) {
        bodyView.text = bodyText
        bodyView.visibility = View.VISIBLE
    } else {
        bodyView.visibility = View.GONE
    }
    adView.bodyView = bodyView

    // 4. Call to Action Button
    val ctaView = adView.findViewById<Button>(R.id.ad_call_to_action)
    val ctaText = nativeAd.callToAction
    ctaView.text = if (!ctaText.isNullOrBlank()) ctaText else adView.context.getString(R.string.ad_cta_default)
    ctaView.visibility = View.VISIBLE
    adView.callToActionView = ctaView

    // 5. Advertiser
    val advertiserView = adView.findViewById<TextView>(R.id.ad_advertiser)
    val advertiserText = nativeAd.advertiser
    if (!advertiserText.isNullOrBlank()) {
        advertiserView.text = advertiserText
        advertiserView.visibility = View.VISIBLE
    } else {
        advertiserView.visibility = View.GONE
    }
    adView.advertiserView = advertiserView

    // 6. Star Rating
    val ratingBar = adView.findViewById<RatingBar>(R.id.ad_stars)
    val starRating = nativeAd.starRating
    if (starRating != null && starRating > 0) {
        ratingBar.rating = starRating.toFloat()
        ratingBar.visibility = View.VISIBLE
    } else {
        ratingBar.visibility = View.GONE
    }
    adView.starRatingView = ratingBar

    // 7. Store
    val storeView = adView.findViewById<TextView>(R.id.ad_store)
    val storeText = nativeAd.store
    if (!storeText.isNullOrBlank()) {
        storeView.text = storeText
        storeView.visibility = View.VISIBLE
    } else {
        storeView.visibility = View.GONE
    }
    adView.storeView = storeView

    // 8. Price
    val priceView = adView.findViewById<TextView>(R.id.ad_price)
    val priceText = nativeAd.price
    if (!priceText.isNullOrBlank()) {
        priceView.text = priceText
        priceView.visibility = View.VISIBLE
    } else {
        priceView.visibility = View.GONE
    }
    adView.priceView = priceView

    // 9. MediaView (Video / Rich Image)
    val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
    val mediaContent = nativeAd.mediaContent
    if (mediaContent != null && (mediaContent.hasVideoContent() || mediaContent.mainImage != null)) {
        mediaView.mediaContent = mediaContent
        mediaView.visibility = View.VISIBLE
    } else {
        mediaView.visibility = View.GONE
    }
    adView.mediaView = mediaView

    // Register Native Ad object with NativeAdView
    adView.setNativeAd(nativeAd)
}

@Composable
private fun NativeAdPlaceholder(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFFE67E22),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Ad",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sponsored App or Service",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Experience high-performance, seamless privacy-first utilities directly on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdMobNativeCardPreview() {
    LinkScrubTheme {
        AdMobNativeCard(modifier = Modifier.padding(16.dp))
    }
}
