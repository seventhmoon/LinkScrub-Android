package com.androidfung.linkscrub.ui

import android.app.Application
import com.androidfung.linkscrub.data.model.OpenGraphMetadata
import com.androidfung.linkscrub.data.network.OpenGraphScraper
import com.androidfung.linkscrub.data.network.RedirectUnshortener
import com.androidfung.linkscrub.data.network.UnshortenResult
import com.androidfung.linkscrub.data.repository.HistoryRepository
import com.androidfung.linkscrub.domain.CleanUrlUseCase
import com.androidfung.linkscrub.domain.UrlCleaner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LinkScrubViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val application: Application = Application()

    private val fakeRedirectUnshortener = object : RedirectUnshortener {
        override suspend fun unshorten(url: String, maxHops: Int): UnshortenResult {
            return if (url.contains("bit.ly")) {
                UnshortenResult(
                    initialUrl = url,
                    finalUrl = "https://example.com/article?utm_source=twitter&utm_medium=social&id=42",
                    redirectChain = listOf("https://example.com/article?utm_source=twitter&utm_medium=social&id=42"),
                    isRedirected = true
                )
            } else {
                UnshortenResult(
                    initialUrl = url,
                    finalUrl = url,
                    redirectChain = emptyList(),
                    isRedirected = false
                )
            }
        }
    }

    private val fakeOpenGraphScraper = object : OpenGraphScraper {
        override suspend fun scrape(url: String): OpenGraphMetadata {
            return OpenGraphMetadata(
                title = "Clean Link Title",
                description = "Clean Link Description",
                imageUrl = "https://example.com/thumb.jpg",
                siteName = "example.com"
            )
        }
    }

    private val fakeHistoryRepository = object : HistoryRepository {
        override fun getAllHistory() = flowOf(emptyList<com.androidfung.linkscrub.data.model.HistoryItem>())
        override suspend fun getHistoryById(id: Long) = null
        override fun searchHistory(query: String) = flowOf(emptyList<com.androidfung.linkscrub.data.model.HistoryItem>())
        override suspend fun saveCleanedUrl(
            originalUrl: String,
            unshortenedUrl: String?,
            cleanedUrl: String,
            removedParams: List<String>,
            metadata: OpenGraphMetadata?
        ): Long = 1L
        override suspend fun delete(id: Long) {}
        override suspend fun clearAll() {}
    }

    private lateinit var viewModel: LinkScrubViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val urlCleaner = UrlCleaner()
        val cleanUrlUseCase = CleanUrlUseCase(
            urlCleaner = urlCleaner,
            redirectUnshortener = fakeRedirectUnshortener,
            openGraphScraper = fakeOpenGraphScraper,
            historyRepository = fakeHistoryRepository
        )

        viewModel = LinkScrubViewModel(
            application = application,
            urlCleaner = urlCleaner,
            redirectUnshortener = fakeRedirectUnshortener,
            openGraphScraper = fakeOpenGraphScraper,
            historyRepository = fakeHistoryRepository,
            cleanUrlUseCase = cleanUrlUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isStep1Input() {
        val state = viewModel.uiState.value
        assertEquals(WizardStep.INPUT, state.currentStep)
        assertEquals("", state.inputUrl)
        assertFalse(state.isQuickCleanEnabled)
    }

    @Test
    fun guidedFlow_step1ToStep2_inspectsParametersAndUnshortens() = runTest {
        viewModel.onInputUrlChanged("https://bit.ly/test123")
        viewModel.onProceedFromInput()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(WizardStep.INSPECT, state.currentStep)
        assertTrue(state.isRedirected)
        assertEquals("https://example.com/article?utm_source=twitter&utm_medium=social&id=42", state.unshortenedUrl)
        // 3 parameters: utm_source, utm_medium, id
        assertEquals(3, state.detectedParameters.size)

        val utmSourceChip = state.detectedParameters.find { it.key == "utm_source" }
        assertNotNull(utmSourceChip)
        assertTrue(utmSourceChip!!.isTracking)
        assertTrue(utmSourceChip.isMarkedForRemoval)

        val idChip = state.detectedParameters.find { it.key == "id" }
        assertNotNull(idChip)
        assertFalse(idChip!!.isTracking)
        assertFalse(idChip.isMarkedForRemoval)
    }

    @Test
    fun step2_toggleParameterRemoval_updatesCandidateUrl() = runTest {
        viewModel.onInputUrlChanged("https://example.com?utm_source=tw&custom=value")
        viewModel.onProceedFromInput()
        advanceUntilIdle()

        // Toggle custom parameter to be removed
        viewModel.toggleParameterRemoval("custom")
        val state = viewModel.uiState.value
        val customChip = state.detectedParameters.find { it.key == "custom" }
        assertTrue(customChip!!.isMarkedForRemoval)
        assertEquals(2, state.removedParamsCount)
        assertFalse(state.cleanedUrl.contains("custom=value"))
        assertFalse(state.cleanedUrl.contains("utm_source=tw"))

        // Reset all parameters
        viewModel.resetAllParameters()
        val resetState = viewModel.uiState.value
        assertEquals(0, resetState.removedParamsCount)
        assertTrue(resetState.cleanedUrl.contains("utm_source=tw"))
        assertTrue(resetState.cleanedUrl.contains("custom=value"))
    }

    @Test
    fun guidedFlow_step2ToStep3_fetchesMetadataAndSetsPreview() = runTest {
        viewModel.onInputUrlChanged("https://example.com?utm_source=twitter")
        viewModel.onProceedFromInput()
        advanceUntilIdle()

        viewModel.onProceedToPreview()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(WizardStep.PREVIEW, state.currentStep)
        assertEquals("https://example.com/", state.cleanedUrl)
        assertNotNull(state.metadata)
        assertEquals("Clean Link Title", state.metadata?.title)
        assertEquals("Clean Link Description", state.metadata?.description)
    }

    @Test
    fun quickCleanMode_directlySanitizesAndJumpsToPreview() = runTest {
        viewModel.onInputUrlChanged("https://example.com?utm_source=tw&gclid=xyz")
        viewModel.onQuickCleanClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(WizardStep.PREVIEW, state.currentStep)
        assertEquals("https://example.com/", state.cleanedUrl)
        assertEquals(2, state.removedParamsCount)
        assertNotNull(state.metadata)
    }

    @Test
    fun tuneInspectAction_inspectsUrlParameters() = runTest {
        viewModel.onInputUrlChanged("https://example.com?utm_source=tw&gclid=xyz")
        viewModel.onTuneInspectClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(WizardStep.INSPECT, state.currentStep)
        assertEquals(2, state.detectedParameters.size)
    }

    @Test
    fun clearInput_resetsAllStateToInitial() = runTest {
        viewModel.onInputUrlChanged("https://example.com?utm_source=tw")
        viewModel.onProceedFromInput()
        advanceUntilIdle()

        viewModel.clearInput()
        val state = viewModel.uiState.value
        assertEquals("", state.inputUrl)
        assertEquals(WizardStep.INPUT, state.currentStep)
        assertTrue(state.detectedParameters.isEmpty())
        assertEquals("", state.cleanedUrl)
        assertNull(state.unshortenedUrl)
    }

    @Test
    fun handleSharedUrl_guidedMode_inspectsUrl() = runTest {
        viewModel.setQuickCleanEnabled(false)
        viewModel.handleSharedUrl("Check out this link: https://example.com/item?utm_source=twitter&gclid=123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(WizardStep.INSPECT, state.currentStep)
        assertEquals("https://example.com/item?utm_source=twitter&gclid=123", state.inputUrl)
        assertEquals(2, state.detectedParameters.size)
    }

    @Test
    fun handleSharedUrl_quickCleanMode_immediatelyCleansAndPreview() = runTest {
        viewModel.setQuickCleanEnabled(true)
        viewModel.handleSharedUrl("https://example.com/item?utm_source=share&utm_medium=android")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(WizardStep.PREVIEW, state.currentStep)
        assertEquals("https://example.com/item", state.cleanedUrl)
        assertTrue(state.isQuickCleanCopiedNotification)
    }

    @Test
    fun historySelection_loadsIntoPreview() = runTest {
        val historyItem = com.androidfung.linkscrub.data.model.HistoryItem(
            id = 10L,
            originalUrl = "https://example.com?fbclid=xyz",
            unshortenedUrl = null,
            cleanedUrl = "https://example.com",
            removedParams = listOf("fbclid"),
            metadata = OpenGraphMetadata(title = "History Title", siteName = "example.com")
        )

        viewModel.selectHistoryItem(historyItem)
        val state = viewModel.uiState.value
        assertEquals(WizardStep.PREVIEW, state.currentStep)
        assertEquals("https://example.com", state.cleanedUrl)
        assertEquals("History Title", state.metadata?.title)
    }

    @Test
    fun historySearch_updatesQuery() = runTest {
        viewModel.onHistorySearchQueryChanged("example")
        val state = viewModel.uiState.value
        assertEquals("example", state.historySearchQuery)
    }

    @Test
    fun historyPaneToggle_updatesVisibility() = runTest {
        assertFalse(viewModel.uiState.value.isHistoryPaneVisible)
        viewModel.toggleHistoryPane()
        assertTrue(viewModel.uiState.value.isHistoryPaneVisible)
        viewModel.setHistoryPaneVisible(false)
        assertFalse(viewModel.uiState.value.isHistoryPaneVisible)
    }
}
