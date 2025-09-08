package com.segment.analytics.kotlin.destinations

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.segment.analytics.kotlin.destinations.clevertap.BuildConfig
import com.clevertap.android.sdk.CleverTapAPI
import com.segment.analytics.kotlin.android.plugins.AndroidLifecycle
import com.segment.analytics.kotlin.core.AliasEvent
import com.segment.analytics.kotlin.core.BaseEvent
import com.segment.analytics.kotlin.core.IdentifyEvent
import com.segment.analytics.kotlin.core.ScreenEvent
import com.segment.analytics.kotlin.core.Settings
import com.segment.analytics.kotlin.core.TrackEvent
import com.segment.analytics.kotlin.core.platform.DestinationPlugin
import com.segment.analytics.kotlin.core.platform.Plugin
import com.segment.analytics.kotlin.core.platform.plugins.logger.LogKind
import com.segment.analytics.kotlin.core.platform.plugins.logger.log
import com.segment.analytics.kotlin.core.utilities.toContent
import com.segment.analytics.kotlin.destinations.CleverTapDestination.CleverTapConstants.CHARGED_KEYS
import com.segment.analytics.kotlin.destinations.CleverTapDestination.CleverTapConstants.ERROR_CODE
import com.segment.analytics.kotlin.destinations.CleverTapDestination.CleverTapConstants.FEMALE_TOKENS
import com.segment.analytics.kotlin.destinations.CleverTapDestination.CleverTapConstants.LIBRARY_NAME
import com.segment.analytics.kotlin.destinations.CleverTapDestination.CleverTapConstants.MALE_TOKENS
import com.segment.analytics.kotlin.destinations.CleverTapDestination.CleverTapConstants.ORDER_COMPLETED_KEY
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentLinkedQueue

@Serializable
data class CleverTapSettings(
    @SerialName("clevertap_account_id")
    val accountId: String,
    @SerialName("clevertap_account_token")
    val accountToken: String,
    val region: String,
)

class CleverTapDestination internal constructor(
    private val context: Context,
    private val onInitCompleted: ((CleverTapAPI) -> Unit)? = null,
    private val onInitFailed: ((String) -> Unit)? = null,
    private val mainHandler: Handler
) : DestinationPlugin(), AndroidLifecycle {

    constructor(
        context: Context,
        onInitCompleted: ((CleverTapAPI) -> Unit)? = null,
        onInitFailed: ((String) -> Unit)? = null
    ) : this(context, onInitCompleted, onInitFailed, Handler(Looper.getMainLooper()))

    override val key: String = "CleverTap"

    var cleverTapSettings: CleverTapSettings? = null

    @Volatile
    internal var cl: CleverTapAPI? = null

    // Thread-safe queue for operations before CleverTap is ready
    private val pendingOperations = ConcurrentLinkedQueue<() -> Unit>()

    @SuppressLint("RestrictedApi")
    override fun update(settings: Settings, type: Plugin.UpdateType) {
        super.update(settings, type)

        if (!settings.hasIntegrationSettings(this)) {
            onInitFailed?.invoke("CleverTap integration settings not found")
            return
        }
        analytics.log("CleverTap Destination is enabled")

        cleverTapSettings = settings.destinationSettings(key)
        val settingsData = cleverTapSettings
        if (settingsData == null) {
            analytics.log("CleverTapSettings not available. Not loading CleverTap Destination.")
            onInitFailed?.invoke("CleverTapSettings not available")
            return
        }

        // Only initialize on first update
        if (type != Plugin.UpdateType.Initial) {
            return
        }

        val accountID = settingsData.accountId
        val accountToken = settingsData.accountToken
        val region = settingsData.region.replace(".", "")

        if (accountID.isEmpty() || accountToken.isEmpty()) {
            val reason = "Missing credentials: accountID=${if (accountID.isEmpty()) "empty" else "present"}, accountToken=${if (accountToken.isEmpty()) "empty" else "present"}"
            analytics.log(
                "CleverTap+Segment integration attempted but $reason"
            )
            onInitFailed?.invoke(reason)
            return
        }

        CleverTapAPI.changeCredentials(accountID, accountToken, region)
        cl = CleverTapAPI.getDefaultInstance(context)?.apply {
            setLibrary(LIBRARY_NAME)
            setCustomSdkVersion(LIBRARY_NAME, BuildConfig.VERSION_CODE)
        }

        // Execute any pending operations now that CleverTap is initialized
        if (cl != null && pendingOperations.isNotEmpty()) {
            analytics.log("Executing ${pendingOperations.size} pending CleverTap operations")
            mainHandler.post {
                while (pendingOperations.isNotEmpty()) {
                    val operation = pendingOperations.poll() ?: break
                    try {
                        operation()
                    } catch (t: Throwable) {
                        analytics.log("Error executing pending operation: $t", LogKind.ERROR)
                    }
                }
            }
        }

        analytics.log("Configured CleverTap+Segment integration and initialized CleverTap.")

        // Call the initialization callback with the CleverTap instance if provided
        cl?.let { cleverTapInstance ->
            onInitCompleted?.invoke(cleverTapInstance)
        }
    }

    /**
     * Safely execute CleverTap operations, queuing them if CleverTap isn't initialized yet
     */
    private fun executeCleverTapOperation(operation: () -> Unit) {
        val cleverTapInstance = cl
        if (cleverTapInstance != null) {
            try {
                operation()
            } catch (t: Throwable) {
                analytics.log("CleverTap operation failed: $t", LogKind.ERROR)
            }
        } else {
            analytics.log("CleverTap not initialized yet, queuing operation")
            pendingOperations.offer(operation)
        }
    }

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
        super.onActivityCreated(activity, savedInstanceState)

        activity?.let { act ->
            val currentActivityRef = WeakReference(act)

            executeCleverTapOperation {
                analytics.log("Executing onActivityCreated")
                CleverTapAPI.setAppForeground(true)

                currentActivityRef.get()?.intent?.let { intent ->
                    cl?.pushNotificationClickedEvent(intent.extras)
                    cl?.pushDeepLink(intent.data)
                }
            }
        }
    }


    override fun onActivityResumed(activity: Activity?) {
        super.onActivityResumed(activity)

        activity?.let { act ->
            val currentActivityRef = WeakReference(act)

            executeCleverTapOperation {
                analytics.log("Executing onActivityResumed")
                CleverTapAPI.onActivityResumed(currentActivityRef.get())
            }
        }
    }

    override fun onActivityPaused(activity: Activity?) {
        super.onActivityPaused(activity)

        executeCleverTapOperation {
            analytics.log("Executing onActivityPaused")
            CleverTapAPI.onActivityPaused()
        }
    }

    override fun alias(payload: AliasEvent): BaseEvent? {
        if (payload.userId.isEmpty()) {
            return payload
        }
        try {
            val profile = HashMap<String, Any>()
            profile.put("Identity", payload.userId)
            cl?.pushProfile(profile)
        } catch (t: Throwable) {
            analytics.log("CleverTap: Error pushing profile $t", LogKind.ERROR)
            cl?.pushError(t.message, ERROR_CODE)
        }
        return payload
    }

    override fun screen(payload: ScreenEvent): BaseEvent? {
        try {
            cl?.recordScreen(payload.name)
        } catch (t: Throwable) {
            analytics.log("Screen event failed $t", LogKind.ERROR)
        }
        return payload
    }

    override fun track(payload: TrackEvent): BaseEvent? {
        super.track(payload)
        val event: String = payload.event

        if (event.isBlank()) {
            analytics.log("CleverTap: Event Name is blank, returning")
            return payload
        }

        val properties = payload.properties.toPrimitive()
        if (event.equals(ORDER_COMPLETED_KEY, ignoreCase = true)) {
            handleOrderCompleted(properties)
            return payload
        }

        try {
            cl?.pushEvent(event, properties)
        } catch (t: Throwable) {
            analytics.log("CleverTap: Error pushing event $t", LogKind.ERROR)
            cl?.pushError(t.message, ERROR_CODE)
        }
        return payload
    }

    private fun handleOrderCompleted(properties: Map<String, Any?>) {
        val details = HashMap<String, Any>()
        for (key in properties.keys) {
            if (CHARGED_KEYS.contains(key.lowercase())) continue
            try {
                properties[key]?.let { value ->
                    details[key] = value
                }
            } catch (t: Throwable) {
                analytics.log("CleverTap: Error adding $key to properties due to $t", LogKind.ERROR)
            }
        }

        val items = (properties["products"] as? ArrayList<HashMap<String, Any>>) ?: arrayListOf()

        try {
            cl?.pushChargedEvent(details, items)
        } catch (t: Throwable) {
            analytics.log("CleverTap: Error handling Order Completed")
            cl?.pushError("Error handling Order Completed: " + t.message, ERROR_CODE)
        }
    }


    override fun identify(payload: IdentifyEvent): BaseEvent? {
        super.identify(payload)

        val traits = payload.traits.toPrimitive()
        try {
            val profile = transform<Any?>(
                traits,
                CleverTapConstants.MAP_KNOWN_PROFILE_FIELDS
            )

            val userId = payload.userId
            if (userId.isNotBlank()) {
                profile["Identity"] = userId
            }

            normalizeGender(profile["Gender"]?.toString())?.let {
                profile["Gender"] = it
            }
            cl?.onUserLogin(profile)
        } catch (t: Throwable) {
            analytics.log("CleverTap: Error pushing profile $t", LogKind.ERROR)
            cl?.pushError(t.message, ERROR_CODE)
        }
        return payload
    }

    private fun normalizeGender(gender: String?): String? = when (gender?.uppercase()) {
        in MALE_TOKENS -> "M"
        in FEMALE_TOKENS -> "F"
        else -> null
    }

    private fun <T> transform(input: Map<String, T>, mapper: Map<String, String>) =
        input.mapKeys { (key, _) -> mapper[key] ?: key }.toMutableMap()

    private fun JsonObject.toPrimitive(): Map<String, Any?> {
        return this.mapValues { (_, v) -> v.toContent() }
    }

    object CleverTapConstants {
        const val ORDER_COMPLETED_KEY = "Order Completed"
        const val ERROR_CODE = 512
        const val LIBRARY_NAME = "CleverTap"

        val MALE_TOKENS = setOf("M", "MALE")
        val FEMALE_TOKENS = setOf("F", "FEMALE")
        val MAP_KNOWN_PROFILE_FIELDS: Map<String, String> = linkedMapOf(
            "phone" to "Phone",
            "name" to "Name",
            "email" to "Email",
            "birthday" to "DOB",
            "gender" to "Gender"
        )

        val CHARGED_KEYS: Set<String> = setOf("products")
    }
}