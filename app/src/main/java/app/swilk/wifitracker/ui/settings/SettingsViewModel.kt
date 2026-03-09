package app.swilk.wifitracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.swilk.wifitracker.util.LocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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

    val showDays: StateFlow<Boolean> = localeManager.showDaysFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, localeManager.showDaysFlow.value)

    fun setShowDays(showDays: Boolean) {
        localeManager.setShowDays(showDays)
    }
}
