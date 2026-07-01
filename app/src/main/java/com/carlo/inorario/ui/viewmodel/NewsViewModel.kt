package com.carlo.inorario.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carlo.inorario.data.model.NewsItem
import com.carlo.inorario.data.network.NetworkClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.carlo.inorario.data.local.DataStoreManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class NewsViewModel(private val dataStoreManager: DataStoreManager) : ViewModel() {
    private val _newsItems = MutableStateFlow<List<NewsItem>>(emptyList())
    val newsItems: StateFlow<List<NewsItem>> = _newsItems

    private val _isLoading = MutableStateFlow(value = true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        viewModelScope.launch {
            combine(
                dataStoreManager.strikeRegionFlow,
                dataStoreManager.hasSupportFlow
            ) { region, hasSupport ->
                if (hasSupport) region else "Tutte"
            }.collectLatest { effectiveRegion ->
                fetchNews(effectiveRegion)
            }
        }
    }

    private fun fetchNews(region: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Check cache first
                val cacheTime = dataStoreManager.newsCacheTimeFlow.first()
                val cacheRegion = dataStoreManager.newsCacheRegionFlow.first()
                val currentTime = System.currentTimeMillis()
                val cacheValidMs = 12 * 60 * 60 * 1000L // 12 hours

                if (currentTime - cacheTime < cacheValidMs && cacheRegion == region) {
                    val cacheJson = dataStoreManager.newsCacheJsonFlow.first()
                    if (!cacheJson.isNullOrEmpty()) {
                        val type = object : TypeToken<List<NewsItem>>() {}.type
                        val cachedNews: List<NewsItem>? = Gson().fromJson(cacheJson, type)
                        if (cachedNews != null && cachedNews.isNotEmpty()) {
                            _newsItems.value = cachedNews
                            _isLoading.value = false
                            return@launch
                        }
                    }
                }

                // If cache is invalid or missing, fetch from network
                val fetchedNews = NetworkClient.backendService.getNews(region)
                _newsItems.value = fetchedNews

                // Save to cache
                dataStoreManager.saveNewsCache(Gson().toJson(fetchedNews), currentTime, region)

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
