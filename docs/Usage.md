# CleverTap Analytics Kotlin SDK - Usage Guide

This guide covers the basic usage of the CleverTap Analytics Kotlin SDK integration with Segment Analytics.

## 1. Integration

Add the following dependencies to your `build.gradle.kts` file:

```kotlin
dependencies {
    // CleverTap SDK
    implementation("com.clevertap.android:clevertap-android-sdk:7.5.1")
    
    // Segment Kotlin Analytics
    implementation("com.segment.analytics.kotlin:android:1.21.0")
    
    // CleverTap Destination 
    implementation("com.clevertap.android:clevertap-segment.kotlin:1.0.0") // todo change based on the published namespace
}
```

## 2. Initialisation

Initialize the SDK:

```kotlin
class MyApplication : Application() {
    companion object {
        private const val WRITE_KEY = "your_segment_write_key"
    }

    override fun onCreate() {
        CleverTapAPI.setDebugLevel(CleverTapAPI.LogLevel.VERBOSE)
        Analytics.debugLogsEnabled = true
        ActivityLifecycleCallback.register(this)
        super.onCreate()

        // Initialize Segment Analytics with CleverTap destination
        analytics = Analytics(WRITE_KEY, applicationContext).apply {
            this.add(plugin = CleverTapDestination(applicationContext, ::cleverTapReady))
        }
    }

    private fun cleverTapReady(cleverTapInstance: CleverTapAPI) {
        // The clevertap instance received here after Segment Initialisation can be accessed to use other clevertap features such as AppInbox, NativeDisplay etc.
    }
}
```

## 3. Record an Event
Segment's `track` API is mapped to CleverTap's `pushEvent`.

```kotlin
val properties = HashMap<String, Any>()
properties["testString"] = "Electronics"
properties["testDecimal"] = 299.99
properties["testInteger"] = 2
properties["testDate"] = CleverTapUtils.getClevertapDate(
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse("01/12/1991") ?: Date())

analytics.track("testEvent", properties)
```

## 4. Record a Charged Event
Events tracked using the name `Order Completed` is mapped to CleverTap’s `Charged` event.

```kotlin
val product1 = HashMap<String, Any>()
product1["id"] = "product_001"
product1["sku"] = "SKU001"
product1["price"] = 100.0

val product2 = HashMap<String, Any>()
product2["id"] = "product_002"
product2["sku"] = "SKU002"  
product2["price"] = 200.0

val products = arrayListOf(product1, product2)

// Track order completed event
val properties = HashMap<String, Any>()
properties["orderId"] = "ORDER_12345"
properties["revenue"] = 300.0
properties["products"] = products

analytics?.track("Order Completed", properties)
```

## 5. Record User Information
Segment's `identify` API is mapped to CleverTap's `onUserLogin`.

```kotlin
val userId = "user_123"
val traits = HashMap<String, Any>()
traits["name"] = "John Doe"
traits["email"] = "john@example.com"
traits["phone"] = "+1234567890"
traits["gender"] = "M"
traits["age"] = 30

analytics?.identify(userId, traits)
```

You can also use the alias method for user identification:

```kotlin
analytics?.alias("new_user_id")
```

## 6. Record Screen
Segment's `screen` API is mapped to CleverTap's `recordScreen`.

```kotlin
analytics?.screen("Home Screen")
```

## Additional Features

### CleverTap Specific Features
Access CleverTap-specific features through the CleverTap instance

Refer to the CleverTap SDK [documentation](https://developer.clevertap.com/docs/android) for more details.
