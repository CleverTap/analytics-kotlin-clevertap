package com.segment.analytics.kotlin.destinations

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.clevertap.android.sdk.BuildConfig
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.isNotNullAndBlank
import com.segment.analytics.kotlin.android.plugins.AndroidLifecycle
import com.segment.analytics.kotlin.core.AliasEvent
import com.segment.analytics.kotlin.core.BaseEvent
import com.segment.analytics.kotlin.core.IdentifyEvent
import com.segment.analytics.kotlin.core.Properties
import com.segment.analytics.kotlin.core.ScreenEvent
import com.segment.analytics.kotlin.core.Settings
import com.segment.analytics.kotlin.core.TrackEvent
import com.segment.analytics.kotlin.core.Traits
import com.segment.analytics.kotlin.core.platform.DestinationPlugin
import com.segment.analytics.kotlin.core.platform.Plugin
import com.segment.analytics.kotlin.core.platform.plugins.logger.LogKind
import com.segment.analytics.kotlin.core.platform.plugins.logger.log
import com.segment.analytics.kotlin.core.utilities.getDouble
import com.segment.analytics.kotlin.core.utilities.getMapList
import com.segment.analytics.kotlin.core.utilities.getString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlin.collections.orEmpty

@Serializable
data class CleverTapSettings(
    @SerialName("clevertap_account_id")
    val accountId: String,
    @SerialName("clevertap_account_token")
    val accountToken: String,
    val region: String,
)

class CleverTapDestination(private val context: Context) : DestinationPlugin(), AndroidLifecycle {
    override val key: String = "CleverTap"

    var cleverTapSettings: CleverTapSettings? = null

    internal var cl: CleverTapAPI? = null

    @SuppressLint("RestrictedApi")
    override fun update(settings: Settings, type: Plugin.UpdateType) {
        super.update(settings, type)

        if (!settings.hasIntegrationSettings(this)) {
            return
        }
        analytics.log("CleverTap Destination is enabled")

        cleverTapSettings = settings.destinationSettings(key)
        val settingsData = cleverTapSettings
        if (settingsData == null) {
            analytics.log("CleverTapSettings not available. Not loading CleverTap Destination.")
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
            analytics.log(
                "CleverTap+Segment integration attempted initialization without account ID or account token."
            )
            return
        }

        CleverTapAPI.changeCredentials(accountID, accountToken, region)
        cl = CleverTapAPI.getDefaultInstance(context)?.apply {
            setLibrary("Segment-Android")
            setCustomSdkVersion("Segment-Android", BuildConfig.VERSION_CODE)
        }

        analytics.log("Configured CleverTap+Segment integration and initialized CleverTap.")
    }

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
        super.onActivityCreated(activity, savedInstanceState)
        CleverTapAPI.setAppForeground(true)
        try {
            cl?.pushNotificationClickedEvent(activity?.intent?.extras)
        } catch (_: Throwable) {
            // Ignore
        }

        try {
            val intent = activity?.intent
            val data = intent?.data
            cl?.pushDeepLink(data)
        } catch (_: Throwable) {
            // Ignore
        }
    }


    override fun onActivityResumed(activity: Activity?) {
        super.onActivityResumed(activity)
        try {
            CleverTapAPI.onActivityResumed(activity)
        } catch (_: Throwable) {
            // Ignore
        }
    }

    override fun onActivityPaused(activity: Activity?) {
        super.onActivityPaused(activity)
        try {
            CleverTapAPI.onActivityPaused()
        } catch (_: Throwable) {
            // Ignore
        }
    }

    override fun alias(payload: AliasEvent): BaseEvent? {
        if (payload.userId.isEmpty()) {
            return payload
        }
        try {
            val profile = HashMap<String?, Any?>()
            profile.put("Identity", payload.userId)
            cl?.pushProfile(profile)
        } catch (t: Throwable) {
            analytics.log("CleverTap: Error pushing profile $t", LogKind.ERROR)
            cl?.pushError(t.message, 512)
        }
        return payload
    }

    override fun screen(payload: ScreenEvent): BaseEvent? {
        try {
            cl?.recordScreen(payload.name)
        } catch (npe: NullPointerException) {
            analytics.log("ScreenPayLoad obj is null. $npe", LogKind.ERROR)
        }
        return payload
    }

    override fun track(payload: TrackEvent): BaseEvent? {
        super.track(payload)
        val event: String = payload.event

        if (event == "Order Completed") {
            handleOrderCompleted(payload)
            return payload
        }

        val properties: Properties = payload.properties

        try {
            cl?.pushEvent(event, properties)
        } catch (t: Throwable) {
            analytics.log("CleverTap: Error pushing event $t", LogKind.ERROR)
            cl?.pushError(t.message, 512)
        }
        return payload
    }


    private fun handleOrderCompleted(payload: TrackEvent) {
        val properties = payload.properties

        val details = HashMap<String, Any>()
        val items = ArrayList<HashMap<String, Any>>()

        details.put("Amount", getTotal(properties))

        val orderId = properties.getString("orderId")
        if (orderId != null) {
            details.put("Charged ID", orderId)
        }

        for (key in properties.keys) {
            if (key == "products") continue
            try {
                properties[key]?.let { value ->
                    details[key] = value
                }
            } catch (_: Throwable) {
                // optionally log t
            }
        }
        val products = properties.getMapList("products").orEmpty()

        for (product in products) {
            try {
                val item = HashMap<String, Any>()

                product["id"]?.let { item["id"] = it }
                product["name"]?.let { item["name"] = it }
                product["sku"]?.let { item["sku"] = it }
                product["price"]?.let { item["price"] = it }

                items.add(item)
            } catch (t: Throwable) {
                analytics.log("CleverTap: Error handling Order Completed product $t", LogKind.ERROR)
                cl?.pushError("Error handling Order Completed product: ${t.message}", 512)
            }
        }

        try {
            cl?.pushChargedEvent(details, items)
        } catch (t: Throwable) {
            analytics.log("CleverTap: Error handling Order Completed")
            cl?.pushError("Error handling Order Completed: " + t.message, 512)
        }
    }

    private fun getTotal(properties: JsonObject): Double {
        val total: Double? = properties.getDouble("total")
        if (total != null) {
            return total
        }
        val revenue: Double? = properties.getDouble("revenue")
        if (revenue != null) {
            return revenue
        }

        val value: Double? = properties.getDouble("value")
        return value ?: 0.0
    }


    override fun identify(payload: IdentifyEvent): BaseEvent? {

        super.identify(payload)

        val traits: Traits = payload.traits

        try {
            val profile = transform<Any?>(
                traits,
                CleverTapConstants.MAP_KNOWN_PROFILE_FIELDS
            )

            val userId = payload.userId
            if (userId.isNotNullAndBlank()) {
                profile["Identity"] =  userId
            }

            val gender = traits.getString("gender")
            if (gender.isNotNullAndBlank()) {
                if (CleverTapConstants.MALE_TOKENS.contains(gender.uppercase())) {
                    profile.put("Gender", "M")
                } else if (CleverTapConstants.FEMALE_TOKENS.contains(gender.uppercase())) {
                    profile.put("Gender", "F")
                }
            }
            cl?.onUserLogin(profile)
        } catch (t: Throwable) {
            analytics.log("CleverTap: Error pushing profile $t", LogKind.ERROR)
            cl?.pushError(t.message, 512)
        }
        return payload
    }

    fun <T> transform(input: Map<String, T>, mapper: Map<String, String>): MutableMap<String, T> =
        buildMap(input.size) {
            for ((key, value) in input) {
                val mappedKey = mapper[key]
                if (mappedKey.isNullOrEmpty()) {
                    put(key, value) // keep the original key
                } else {
                    put(mappedKey, value)
                }
            }
        }.toMutableMap()


    object CleverTapConstants {
        val MALE_TOKENS = setOf("M", "MALE")
        val FEMALE_TOKENS = setOf("F", "FEMALE")

        val MAP_KNOWN_PROFILE_FIELDS: Map<String, String> = linkedMapOf(
            "phone" to "Phone",
            "name" to "Name",
            "email" to "Email",
            "birthday" to "DOB"
        )
    }
}