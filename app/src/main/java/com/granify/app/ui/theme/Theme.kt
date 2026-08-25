package com.granify.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.granify.app.R

// Every pair below is verified against WCAG contrast, not left to Material3's baseline
// defaults (which are tuned for a neutral gray palette and don't match this warm one, and
// were never checked). All currently pass AAA (7:1) for normal text; `outline` only needs
// the lower 3:1 non-text threshold and clears it with room to spare. See
// docs/DECISIONS.md, "Color palette and contrast".
private val OumatjieColors = lightColorScheme(
    primary = Color(0xFF174E3A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F4E5),
    onPrimaryContainer = Color(0xFF082C20),
    secondaryContainer = Color(0xFFFFE7B3),
    onSecondaryContainer = Color(0xFF392D00),
    background = Color(0xFFFFFDF7),
    onBackground = Color(0xFF1C1B18),
    surface = Color(0xFFFFFDF7),
    onSurface = Color(0xFF1C1B18),
    surfaceVariant = Color(0xFFE8E2D6),
    onSurfaceVariant = Color(0xFF49443A),
    error = Color(0xFF9B1C1C),
    errorContainer = Color(0xFFFBE4E1),
    onErrorContainer = Color(0xFF5A1414),
    outline = Color(0xFF756C5C),
)

// Designed by the Braille Institute of America specifically for low-vision and aging
// readers (distinct letterforms for commonly-confused characters — I/l/1, 0/O). OFL-licensed
// (see app/src/main/assets/licenses/atkinson_hyperlegible_OFL.txt). See docs/ROADMAP.md,
// "Design direction: final plan" — Typography.
private val AtkinsonHyperlegible = FontFamily(
    Font(R.font.atkinson_hyperlegible_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.atkinson_hyperlegible_bold, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.atkinson_hyperlegible_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.atkinson_hyperlegible_bolditalic, FontWeight.Bold, FontStyle.Italic),
)

private fun TextStyle.withOumatjieFont(): TextStyle = copy(fontFamily = AtkinsonHyperlegible)

// Every one of Typography's ~15 styles is routed through Atkinson Hyperlegible here, not just
// the 7 that already had a custom size — see docs/DECISIONS.md, "Atkinson Hyperlegible font
// downloaded but not wired in" for why this was left half-done previously and what to avoid
// repeating. Sizes below are unchanged from the prior size-only overrides; only the font
// family is new for every style, including the ones that keep Material3's default size.
private val defaultTypography = Typography()

private val OumatjieTypography = Typography(
    displayLarge = defaultTypography.displayLarge.withOumatjieFont(),
    displayMedium = defaultTypography.displayMedium.withOumatjieFont(),
    displaySmall = defaultTypography.displaySmall.withOumatjieFont()
        .copy(fontSize = 40.sp, lineHeight = 48.sp),
    headlineLarge = defaultTypography.headlineLarge.withOumatjieFont()
        .copy(fontSize = 34.sp, lineHeight = 42.sp),
    headlineMedium = defaultTypography.headlineMedium.withOumatjieFont()
        .copy(fontSize = 30.sp, lineHeight = 38.sp),
    headlineSmall = defaultTypography.headlineSmall.withOumatjieFont()
        .copy(fontSize = 26.sp, lineHeight = 34.sp),
    titleLarge = defaultTypography.titleLarge.withOumatjieFont()
        .copy(fontSize = 23.sp, lineHeight = 31.sp),
    titleMedium = defaultTypography.titleMedium.withOumatjieFont()
        .copy(fontSize = 20.sp, lineHeight = 28.sp),
    titleSmall = defaultTypography.titleSmall.withOumatjieFont(),
    bodyLarge = defaultTypography.bodyLarge.withOumatjieFont()
        .copy(fontSize = 20.sp, lineHeight = 30.sp),
    bodyMedium = defaultTypography.bodyMedium.withOumatjieFont(),
    bodySmall = defaultTypography.bodySmall.withOumatjieFont(),
    labelLarge = defaultTypography.labelLarge.withOumatjieFont(),
    labelMedium = defaultTypography.labelMedium.withOumatjieFont(),
    labelSmall = defaultTypography.labelSmall.withOumatjieFont(),
)

// A deliberate two-radius shape system, not Material3's single uniform corner scale — see
// docs/DESIGN_SYSTEM.md, "Shape". `medium` (16dp) marks informational surfaces (banners, notes,
// the summary card — see `OumatjieInfoCard` in GranifyComponents.kt); `large` (24dp) marks
// primary, tappable content surfaces (an inbox `MailCard`). Two consistently-applied radii read
// as an intentional choice; using whichever RoundedCornerShape value felt right per composable
// (this codebase's own pre-2026-08-24 state: 16dp and 20dp hardcoded ad hoc in different files)
// reads as arbitrary. `extraSmall`/`small` are left at Material3's own defaults, which are
// already small enough not to need a signature treatment (chips, menus).
val OumatjieShapes = Shapes(
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun OumatjieTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OumatjieColors,
        typography = OumatjieTypography,
        shapes = OumatjieShapes,
        content = content,
    )
}
