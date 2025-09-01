package com.clevertap.segment.kotlin.testapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.clevertap.android.sdk.CleverTapAPI
import com.segment.analytics.kotlin.core.Analytics
import com.segment.analytics.kotlin.destinations.CleverTapUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

data class MainUiState(
    val userId: String = "",
    val email: String = "",
    val toastMessage: String? = null
)

class HomeViewModel(
    private val analytics: Analytics?,
    private val getCleverTap: () -> CleverTapAPI?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun updateUserId(userId: String) {
        _uiState.value = _uiState.value.copy(userId = userId)
    }

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun identify() {
        val id = Random.nextInt(100000).toString()
        val userId = _uiState.value.userId.takeIf { it.isNotBlank() } ?: id
        val email = _uiState.value.email.takeIf { it.isNotBlank() } ?: "joe$id@gmail.com"
        val name = "Joe$id"
        val phone = "+91-$id"
        analytics?.identify(
            userId,
            buildJsonObject {
                put("name", name)
                put("email", email)
                put("phone", phone)
                put("gender", "M")
                put("boolean", true)
                put("integer", 50)
                put("float", 1.5)
                put("long", 12345L)
                put("string", "hello")
                put("stringInt", "1")
                put("testStringArr", buildJsonArray {
                    add("one")
                    add("two")
                    add("three")
                })
            })
    }

    fun screen() {
        analytics?.screen("Home Screen")
    }

    fun alias() {
        val userId = _uiState.value.userId.takeIf { it.isNotBlank() } ?: Random.nextInt().toString()

        analytics?.alias(userId)

        showToast("alias() called with user id: $userId")
    }

    fun reset() {
        analytics?.reset()
        showToast("reset() called")
    }

    fun showAppInbox() {
        getCleverTap()?.showAppInbox()
        showToast("Showing AppInbox")
    }

    fun track() {
        analytics?.track("testEvent", buildJsonObject {
            put("value", "testValue")
            put("valueInt", 23)
            put("valueLong", 2334235235L)
            put("valueFloat", 23.4)
            put("valueBoolean", true)
            put("valueInt", 23)
            put("valueInt", 23)
            put("valueDate", CleverTapUtils.getClevertapDate(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse("01/12/1991")?: Date()))
        })
    }

    fun trackOrderCompleted() {
        // Track ecommerce event
        analytics?.track("Order Completed", buildJsonObject {
            put("orderId", "123456")
            put("revenue", 100)
            put("products", buildJsonArray {
                addJsonObject {
                    put("id", "id1")
                    put("sku", "sku1")
                    put("price", 100)
                }
                addJsonObject {
                    put("id", "id2")
                    put("sku", "sku2")
                    put("price", 200)
                }
            })
        })
    }

    private fun showToast(message: String) {
        _uiState.value = _uiState.value.copy(toastMessage = message)
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

}

class MainViewModelFactory(
    private val analytics: Analytics?,
    private val getCleverTap: () -> CleverTapAPI?,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(analytics, getCleverTap) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
