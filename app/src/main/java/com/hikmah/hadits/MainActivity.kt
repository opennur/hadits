package com.hikmah.hadits

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.work.WorkManager
import com.hikmah.hadits.data.HadithRepository
import com.hikmah.hadits.ui.HadithViewModel
import com.hikmah.hadits.ui.HikmahApp
import com.hikmah.hadits.ui.theme.HikmahTheme

class MainActivity : ComponentActivity() {
    private val viewModel: HadithViewModel by viewModels {
        HadithViewModel.Factory(
            repository = HadithRepository.getInstance(applicationContext),
            workManager = WorkManager.getInstance(applicationContext),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("hikmah_settings", MODE_PRIVATE)
        setContent {
            var darkTheme by rememberSaveable {
                mutableStateOf(preferences.getBoolean(KEY_DARK_THEME, false))
            }
            HikmahTheme(darkTheme = darkTheme, dynamicColor = false) {
                HikmahApp(
                    viewModel = viewModel,
                    isDarkTheme = darkTheme,
                    onToggleTheme = {
                        darkTheme = !darkTheme
                        preferences.edit().putBoolean(KEY_DARK_THEME, darkTheme).apply()
                    },
                )
            }
        }
    }

    private companion object {
        const val KEY_DARK_THEME = "dark_theme"
    }
}
