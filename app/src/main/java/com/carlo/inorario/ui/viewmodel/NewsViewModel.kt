package com.carlo.inorario.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carlo.inorario.data.model.NewsItem
import com.carlo.inorario.data.network.NetworkClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NewsViewModel : ViewModel() {
    private val _newsItems = MutableStateFlow<List<NewsItem>>(emptyList())
    val newsItems: StateFlow<List<NewsItem>> = _newsItems

    private val _isLoading = MutableStateFlow(value = true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        fetchNews()
    }

    fun fetchNews() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fetchedNews = NetworkClient.backendService.getNews()
                _newsItems.value = fetchedNews
            } catch (e: Exception) {
                e.printStackTrace()
                // In case of error, just keep the current list or clear it
            } finally {
                _isLoading.value = false
            }
        }
    }
}
