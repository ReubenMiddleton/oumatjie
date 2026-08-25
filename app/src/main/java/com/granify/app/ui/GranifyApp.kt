package com.granify.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granify.app.data.settings.TextScale
import com.granify.app.di.AppContainer
import com.granify.app.ui.navigation.OumatjieNavHost
import com.granify.app.ui.theme.OumatjieTheme

@Composable
fun OumatjieApp(container: AppContainer) {
    val textScale by container.settingsRepository.textScale
        .collectAsStateWithLifecycle(initialValue = TextScale.STANDARD)

    // Multiplies (rather than replaces) the system font scale, so this setting adds to the
    // Android accessibility setting the user already chose instead of overriding it.
    val baseDensity = LocalDensity.current
    val scaledDensity = Density(
        density = baseDensity.density,
        fontScale = baseDensity.fontScale * textScale.multiplier,
    )

    OumatjieTheme {
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            OumatjieNavHost(container = container)
        }
    }
}
