package com.wifitracker.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wifitracker.R
import com.wifitracker.ui.MainActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val activity = LocalContext.current as? MainActivity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.language_preference),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(
                modifier = Modifier.selectableGroup()
            ) {
                LanguageOption(
                    languageName = stringResource(R.string.language_english),
                    languageCode = "en",
                    selected = currentLanguage == "en",
                    onClick = {
                        viewModel.setLanguage("en")
                        activity?.recreate()
                    }
                )

                LanguageOption(
                    languageName = stringResource(R.string.language_german),
                    languageCode = "de",
                    selected = currentLanguage == "de",
                    onClick = {
                        viewModel.setLanguage("de")
                        activity?.recreate()
                    }
                )
            }
        }
    }
}

@Composable
fun LanguageOption(
    languageName: String,
    languageCode: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = languageName,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
