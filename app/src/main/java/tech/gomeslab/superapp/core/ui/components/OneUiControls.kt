package tech.gomeslab.superapp.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import tech.gomeslab.superapp.core.ui.theme.ControlActive
import tech.gomeslab.superapp.core.ui.theme.Spacing
import tech.gomeslab.superapp.core.ui.theme.SwitchThumb

@Composable
fun OneUiSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        thumbContent = {},
        colors = SwitchDefaults.colors(
            checkedThumbColor = SwitchThumb,
            checkedTrackColor = ControlActive,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = SwitchThumb,
            uncheckedTrackColor = MaterialTheme.colorScheme.outline,
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}

/** Bloco arredondado monotônico ("focus block" do One UI) que agrupa linhas de conteúdo. */
@Composable
fun FocusCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(content = content)
    }
}

@Composable
fun OptionRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ControlActive,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun OptionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = Spacing.lg),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}
