package tech.gomeslab.superapp.core.ui.theme

import androidx.compose.ui.graphics.Color

// Design tokens portados de stash (frontend/src/styles/global.css). Tema dark, accent roxo.
// Regra: nunca use hex direto nos composables — consuma sempre estes tokens ou o MaterialTheme.

// Backgrounds
val BgPrimary = Color(0xFF0A0A0F)
val BgSecondary = Color(0xFF12121A)
val BgTertiary = Color(0xFF1A1A28)
val BgHover = Color(0xFF1E1E30)
val Surface = Color(0xFF1C1C2E)
val SurfaceHover = Color(0xFF252540)

// Bordas
val BorderDefault = Color(0xFF2A2A40)
val BorderSubtle = Color(0xFF1E1E30)

// Texto
val TextPrimary = Color(0xFFE8E8F0)
val TextSecondary = Color(0xFF9090A8)
val TextTertiary = Color(0xFF606078)

// Accent (marca)
val Accent = Color(0xFF8B5CF6)
val AccentHover = Color(0xFF7C3AED)
val OnAccent = Color(0xFF0A0A0F)

// Semânticas
val Success = Color(0xFF10B981)
val Warning = Color(0xFFF59E0B)
val Error = Color(0xFFEF4444)
val Info = Color(0xFF3B82F6)
