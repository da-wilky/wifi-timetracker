package com.wifitracker.ui.settings

import androidx.lifecycle.ViewModel
import com.wifitracker.util.LocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val localeManager: LocaleManager
) : ViewModel() {

    private val _currentLanguage = MutableStateFlow(localeManager.getCurrentLanguage())
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    fun setLanguage(languageCode: String) {
        localeManager.setLanguage(languageCode)
        _currentLanguage.value = languageCode
    }
}
