package com.segment.analytics.kotlin.destinations

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.clevertap.android.sdk.CleverTapAPI
import com.segment.analytics.kotlin.core.AliasEvent
import com.segment.analytics.kotlin.core.Analytics
import com.segment.analytics.kotlin.core.IdentifyEvent
import com.segment.analytics.kotlin.core.ScreenEvent
import com.segment.analytics.kotlin.core.Settings
import com.segment.analytics.kotlin.core.TrackEvent
import com.segment.analytics.kotlin.core.platform.Plugin
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CleverTapDestinationTest {

    @MockK
    private lateinit var mockContext: Context

    @MockK
    private lateinit var mockActivity: Activity

    @MockK
    private lateinit var mockIntent: Intent

    @MockK
    private lateinit var mockUri: Uri

    @MockK
    private lateinit var mockBundle: Bundle

    @RelaxedMockK
    private lateinit var mockCleverTapAPI: CleverTapAPI

    @RelaxedMockK
    private lateinit var mockAnalytics: Analytics

    private lateinit var destination: CleverTapDestination

    companion object {
        private const val TEST_ACCOUNT_ID = "test-account-id"
        private const val TEST_ACCOUNT_TOKEN = "test-account-token"
        private const val TEST_REGION = "in1"
    }

    private fun getMockSettings(
        accountId: String = TEST_ACCOUNT_ID,
        accountToken: String = TEST_ACCOUNT_TOKEN,
        region: String = TEST_REGION
    ): Settings {
        val cleverTapJsonSettings = JsonObject(
            content = mapOf(
                "clevertap_account_id" to JsonPrimitive(accountId),
                "clevertap_account_token" to JsonPrimitive(accountToken),
                "region" to JsonPrimitive(region)
            )
        )
        val integrations = JsonObject(mapOf("CleverTap" to cleverTapJsonSettings))
        return Settings(integrations)
    }

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        // Mock static methods
        mockkStatic(CleverTapAPI::class)
        
        // Setup default mock behavior
        every { CleverTapAPI.changeCredentials(any(), any(), any()) } just runs
        every { CleverTapAPI.getDefaultInstance(any()) } returns mockCleverTapAPI
        every { CleverTapAPI.setAppForeground(any()) } just runs
        every { CleverTapAPI.onActivityResumed(any()) } just runs
        every { CleverTapAPI.onActivityPaused() } just runs

        every { mockActivity.intent } returns mockIntent
        every { mockIntent.extras } returns mockBundle
        every { mockIntent.data } returns mockUri

        destination = CleverTapDestination(mockContext)
        destination.analytics = mockAnalytics
        destination.cl = mockCleverTapAPI
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test destination key is CleverTap`() {
        assertEquals("CleverTap", destination.key)
    }

    @Test
    fun `test update with valid settings initializes CleverTap`() {
        // Given
        val settings = getMockSettings()
        destination.cl = null // will be init from setup

        // When
        destination.update(settings, Plugin.UpdateType.Initial)

        // Then
        verify { CleverTapAPI.changeCredentials(TEST_ACCOUNT_ID, TEST_ACCOUNT_TOKEN, "in1") }
        verify { CleverTapAPI.getDefaultInstance(mockContext) }
        verify { mockCleverTapAPI.setLibrary("CleverTap") }
        verify { mockCleverTapAPI.setCustomSdkVersion("CleverTap", any()) }
        assertNotNull(destination.cleverTapSettings)
        assertEquals(TEST_ACCOUNT_ID, destination.cleverTapSettings?.accountId)
        assertEquals(TEST_ACCOUNT_TOKEN, destination.cleverTapSettings?.accountToken)
        assertEquals(TEST_REGION, destination.cleverTapSettings?.region)
    }

    @Test
    fun `test update with no integration settings does not initialize CleverTap`() {
        // Given - empty settings without CleverTap integration
        val emptyIntegrations = JsonObject(mapOf<String, JsonObject>())
        val settings = Settings(emptyIntegrations)

        // When
        destination.update(settings, Plugin.UpdateType.Initial)

        // Then
        verify(exactly = 0) { CleverTapAPI.changeCredentials(any(), any(), any()) }
        verify(exactly = 0) { CleverTapAPI.getDefaultInstance(any()) }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `test update with null settings does not initialize CleverTap`() {
        // Given - settings with CleverTap but missing required fields
        val incompleteSettings = JsonObject(
            content = mapOf(
                "some_other_field" to JsonPrimitive("value")
                // missing clevertap_account_id, clevertap_account_token, region
            )
        )
        val integrations = JsonObject(mapOf("CleverTap" to incompleteSettings))
        val settings = Settings(integrations)

        // When
        assertThrows(MissingFieldException::class.java) {
            destination.update(settings, Plugin.UpdateType.Initial)
        }
    }

    @Test
    fun `test update with empty account ID does not initialize CleverTap`() {
        // Given
        val settings = getMockSettings(accountId = "")

        // When
        destination.update(settings, Plugin.UpdateType.Initial)

        // Then
        verify(exactly = 0) { CleverTapAPI.changeCredentials(any(), any(), any()) }
        verify(exactly = 0) { CleverTapAPI.getDefaultInstance(any()) }
    }

    @Test
    fun `test update with empty account token does not initialize CleverTap`() {
        // Given
        val settings = getMockSettings(accountToken = "")

        // When
        destination.update(settings, Plugin.UpdateType.Initial)

        // Then
        verify(exactly = 0) { CleverTapAPI.changeCredentials(any(), any(), any()) }
        verify(exactly = 0) { CleverTapAPI.getDefaultInstance(any()) }
    }

    @Test
    fun `test update with non-initial type does not re-initialize CleverTap`() {
        val settings = getMockSettings()

        // When - update with refresh type
        destination.update(settings, Plugin.UpdateType.Refresh)

        // Then
        verify(exactly = 0) { CleverTapAPI.changeCredentials(any(), any(), any()) }
    }

    @Test
    fun `test onActivityCreated sets app foreground and processes intent`() {
        // When
        destination.onActivityCreated(mockActivity, mockBundle)

        // Then
        verify { CleverTapAPI.setAppForeground(true) }
        verify { mockCleverTapAPI.pushNotificationClickedEvent(mockBundle) }
        verify { mockCleverTapAPI.pushDeepLink(mockUri) }
    }

    @Test
    fun `test onActivityCreated with null activity does nothing`() {
        destination.onActivityCreated(null, mockBundle)

        // Then
        verify(exactly = 0) { CleverTapAPI.setAppForeground(any()) }
    }

    @Test
    fun `test onActivityResumed calls CleverTap API`() {
        // When
        destination.onActivityResumed(mockActivity)

        // Then
        verify { CleverTapAPI.onActivityResumed(mockActivity) }
    }

    @Test
    fun `test onActivityResumed with null activity does nothing`() {
        // When
        destination.onActivityResumed(null)

        // Then
        verify(exactly = 0) { CleverTapAPI.onActivityResumed(any()) }
    }

    @Test
    fun `test onActivityPaused calls CleverTap API`() {
        // When
        destination.onActivityPaused(mockActivity)

        // Then
        verify { CleverTapAPI.onActivityPaused() }
    }

    @Test
    fun `test alias with valid userId creates profile`() {
        val aliasEvent = mockk<AliasEvent>()
        every { aliasEvent.userId } returns "test-user-id"

        // When
        val result = destination.alias(aliasEvent)

        // Then
        verify {
            mockCleverTapAPI.pushProfile(match { profile ->
                profile["Identity"] == "test-user-id"
            })
        }
        assertEquals(aliasEvent, result)
    }

    @Test
    fun `test alias with empty userId returns event without processing`() {
        val aliasEvent = mockk<AliasEvent>()
        every { aliasEvent.userId } returns ""

        // When
        val result = destination.alias(aliasEvent)

        // Then
        verify(exactly = 0) { mockCleverTapAPI.pushProfile(any()) }
        assertEquals(aliasEvent, result)
    }

    @Test
    fun `test alias handles exceptions gracefully`() {
        every { mockCleverTapAPI.pushProfile(any()) } throws RuntimeException("Test exception")
        val aliasEvent = mockk<AliasEvent>()
        every { aliasEvent.userId } returns "test-user-id"

        // When
        val result = destination.alias(aliasEvent)

        // Then
        verify { mockCleverTapAPI.pushError("Test exception", 512) }
        assertEquals(aliasEvent, result)
    }

    @Test
    fun `test screen records screen name`() {
        val screenEvent = mockk<ScreenEvent>()
        every { screenEvent.name } returns "Home Screen"

        // When
        val result = destination.screen(screenEvent)

        // Then
        verify { mockCleverTapAPI.recordScreen("Home Screen") }
        assertEquals(screenEvent, result)
    }

    @Test
    fun `test screen handles exceptions gracefully`() {
        every { mockCleverTapAPI.recordScreen(any()) } throws RuntimeException("Test exception")
        val screenEvent = mockk<ScreenEvent>()
        every { screenEvent.name } returns "Home Screen"

        // When
        val result = destination.screen(screenEvent)

        // Then - should not crash and return the event
        assertEquals(screenEvent, result)
    }

    @Test
    fun `test track with valid event pushes to CleverTap`() {
        val trackEvent = createTrackEvent("Button Clicked", buildJsonObject {
            put("button_name", JsonPrimitive("submit"))
            put("page", JsonPrimitive("checkout"))
        })

        // When
        val result = destination.track(trackEvent)

        // Then
        verify {
            mockCleverTapAPI.pushEvent("Button Clicked", match { props ->
                props["button_name"] == "submit" && props["page"] == "checkout"
            })
        }
        assertEquals(trackEvent, result)
    }

    @Test
    fun `test track with blank event name returns early`() {
        val trackEvent = createTrackEvent("", buildJsonObject {})

        // When
        val result = destination.track(trackEvent)

        // Then
        verify(exactly = 0) { mockCleverTapAPI.pushEvent(any(), any()) }
        assertEquals(trackEvent, result)
    }

    @Test
    fun `test track with Order Completed event calls pushChargedEvent`() {
        val trackEvent = createTrackEvent("Order Completed", buildJsonObject {
            put("revenue", JsonPrimitive(30.0))
            put("order_id", JsonPrimitive("12345"))
            put("products", buildJsonArray {
                add(buildJsonObject {
                    put("name", JsonPrimitive("Product 1"))
                    put("price", JsonPrimitive(10.0))
                })
                add(buildJsonObject {
                    put("name", JsonPrimitive("Product 2"))
                    put("price", JsonPrimitive(20.0))
                })
            })
        })

        // When
        val result = destination.track(trackEvent)

        // Then
        verify { mockCleverTapAPI.pushChargedEvent(any(), any()) }
        assertEquals(trackEvent, result)
    }

    @Test
    fun `test track handles exceptions gracefully`() {
        every { mockCleverTapAPI.pushEvent(any(), any()) } throws RuntimeException("Test exception")
        val trackEvent = createTrackEvent("Button Clicked", buildJsonObject {})

        // When
        val result = destination.track(trackEvent)

        // Then
        verify { mockCleverTapAPI.pushError("Test exception", 512) }
        assertEquals(trackEvent, result)
    }

    @Test
    fun `test identify with valid user data calls onUserLogin`() {
        val identifyEvent = createIdentifyEvent("user123", buildJsonObject {
            put("email", JsonPrimitive("test@example.com"))
            put("name", JsonPrimitive("John Doe"))
            put("phone", JsonPrimitive("+12-34567890"))
            put("gender", JsonPrimitive("male"))
        })

        // When
        val result = destination.identify(identifyEvent)

        // Then - Match the actual values being passed
        verify {
            mockCleverTapAPI.onUserLogin(match { profile ->
                profile["Identity"] == "user123" &&
                        profile["Email"] == "test@example.com" &&
                        profile["Name"] == "John Doe" &&
                        profile["Phone"] == "+12-34567890" &&
                        profile["Gender"] == "M"
            })
        }
        assertEquals(identifyEvent, result)
    }

    @Test
    fun `test identify with blank userId still processes traits`() {
        val identifyEvent = createIdentifyEvent("", buildJsonObject {
            put("email", JsonPrimitive("test@example.com"))
        })

        // When
        val result = destination.identify(identifyEvent)

        // Then
        verify {
            mockCleverTapAPI.onUserLogin(match { profile ->
                profile["Email"] == "test@example.com" &&
                !profile.containsKey("Identity")
            })
        }
        assertEquals(identifyEvent, result)
    }

    @Test
    fun `test identify normalizes gender correctly`() {
        // Test male gender normalization
        val maleEvent = createIdentifyEvent("user1", buildJsonObject {
            put("gender", JsonPrimitive("MALE"))
        })
        destination.identify(maleEvent)

        verify {
            mockCleverTapAPI.onUserLogin(match { profile ->
                profile["Gender"] == "M"
            })
        }

        clearMocks(mockCleverTapAPI)

        // Test female gender normalization
        val femaleEvent = createIdentifyEvent("user2", buildJsonObject {
            put("gender", JsonPrimitive("f"))
        })
        destination.identify(femaleEvent)

        verify {
            mockCleverTapAPI.onUserLogin(match { profile ->
                profile["Gender"] == "F"
            })
        }

        clearMocks(mockCleverTapAPI)

        // Test unknown gender
        val unknownEvent = createIdentifyEvent("user3", buildJsonObject {
            put("gender", JsonPrimitive("other"))
        })
        destination.identify(unknownEvent)

        verify {
            mockCleverTapAPI.onUserLogin(match { profile ->
                profile["Gender"] == "other"
            })
        }
    }

    @Test
    fun `test identify handles exceptions gracefully`() {
        every { mockCleverTapAPI.onUserLogin(any()) } throws RuntimeException("Test exception")
        val identifyEvent = createIdentifyEvent("user123", buildJsonObject {})

        // When
        val result = destination.identify(identifyEvent)

        // Then
        verify { mockCleverTapAPI.pushError("Test exception", 512) }
        assertEquals(identifyEvent, result)
    }

    @Test
    fun `test operations are queued when CleverTap is not initialized`() {
        // Given - destination without initialization
        val uninitializedDestination = CleverTapDestination(mockContext)
        uninitializedDestination.analytics = mockAnalytics

        // When - try to perform operations before initialization
        uninitializedDestination.onActivityCreated(mockActivity, mockBundle)
        uninitializedDestination.onActivityResumed(mockActivity)
        uninitializedDestination.onActivityPaused(mockActivity)

        // Then - operations should not be called immediately
        verify(exactly = 0) { CleverTapAPI.setAppForeground(any()) }
        verify(exactly = 0) { CleverTapAPI.onActivityResumed(any()) }
        verify(exactly = 0) { CleverTapAPI.onActivityPaused() }
    }


    @Test
    fun `test onInitCompleted callback is called after successful initialization`() {
        // Given
        val onInitCompleted = mockk<(CleverTapAPI) -> Unit>()
        every { onInitCompleted.invoke(any()) } just runs
        
        val destination = CleverTapDestination(mockContext, onInitCompleted)
        destination.analytics = mockAnalytics

        // When
        val settings = getMockSettings()
        destination.update(settings, Plugin.UpdateType.Initial)

        // Then
        verify { onInitCompleted.invoke(mockCleverTapAPI) }
    }

    @Test
    fun `test region string is properly cleaned`() {
        // Given - settings with dots in region
        val settings = getMockSettings(region = "in1.abc.xyz")

        // When
        destination.update(settings, Plugin.UpdateType.Initial)

        // Then - dots should be removed from region
        verify { CleverTapAPI.changeCredentials(TEST_ACCOUNT_ID, TEST_ACCOUNT_TOKEN, "in1abcxyz") }
    }

    // Helper methods for creating events
    private fun createTrackEvent(eventName: String, properties: JsonObject): TrackEvent {
        val trackEvent = mockk<TrackEvent>()
        every { trackEvent.event } returns eventName
        every { trackEvent.properties } returns properties
        return trackEvent
    }

    private fun createIdentifyEvent(userId: String, traits: JsonObject): IdentifyEvent {
        val identifyEvent = mockk<IdentifyEvent>()
        every { identifyEvent.userId } returns userId
        every { identifyEvent.traits } returns traits
        return identifyEvent
    }
}
