package com.carlo.inorario.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carlo.inorario.data.local.DataStoreManager
import com.carlo.inorario.data.network.NetworkClient
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ProfileViewModel(private val dataStoreManager: DataStoreManager) : ViewModel() {

    val remoteNotificationsEnabled: StateFlow<Boolean> = dataStoreManager.remoteNotificationsEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    val notifyOnStationPass: StateFlow<Boolean> = dataStoreManager.notifyOnStationPassFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val fcmToken: StateFlow<String> = dataStoreManager.fcmTokenFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val hasSupport: StateFlow<Boolean> = dataStoreManager.hasSupportFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val strikeRegion: StateFlow<String> = dataStoreManager.strikeRegionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Tutte")

    val strikeNotificationsEnabled: StateFlow<Boolean> = dataStoreManager.strikeNotificationsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val userName: StateFlow<String?> = dataStoreManager.userNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val useSpecialPassanteView: StateFlow<Boolean> = dataStoreManager.useSpecialPassanteViewFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun saveUserName(name: String) {
        viewModelScope.launch {
            dataStoreManager.saveUserName(name)
        }
    }

    fun saveStrikeRegion(region: String) {
        viewModelScope.launch {
            dataStoreManager.saveStrikeRegion(region)
        }
    }

    fun saveStrikeNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveStrikeNotificationsEnabled(enabled)
        }
    }

    fun saveUseSpecialPassanteView(use: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveUseSpecialPassanteView(use)
        }
    }

    fun sendFeedback(
        category: String,
        message: String,
        contact: String,
        onSuccess: () -> Unit,
        onError: () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val jsonObject = JSONObject().apply {
                    put("category", category)
                    put("message", message)
                    put("contact", contact)
                }
                val requestBody = jsonObject.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

                val response = NetworkClient.backendService.sendFeedback(requestBody)
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onError()
            }
        }
    }

    fun saveRemoteNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveRemoteNotificationsEnabled(enabled)
        }
    }

    fun saveNotifyOnStationPass(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveNotifyOnStationPass(enabled)
        }
    }

    fun saveFcmToken(token: String) {
        viewModelScope.launch {
            dataStoreManager.saveFcmToken(token)
        }
    }

    fun saveHasSupport(value: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveHasSupport(value)
        }
    }
}
