package tech.gomeslab.superapp.core.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import tech.gomeslab.superapp.core.ui.components.ExtendedScaffold
import tech.gomeslab.superapp.core.ui.theme.Sizes
import tech.gomeslab.superapp.core.ui.theme.Spacing

private data class MiniApp(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val onOpen: () -> Unit,
)

@Composable
fun HomeScreen(
    onOpenAlarm: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenWorkout: () -> Unit,
) {
    val apps = listOf(
        MiniApp("alarm", "Despertador", Icons.Filled.Alarm, onOpenAlarm),
        MiniApp("reminder", "Lembretes", Icons.Filled.NotificationsActive, onOpenReminders),
        MiniApp("workout", "Treino", Icons.Filled.FitnessCenter, onOpenWorkout),
    )

    ExtendedScaffold(title = "SuperApp") { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.md,
                end = Spacing.md,
                top = innerPadding.calculateTopPadding() + Spacing.xs,
                bottom = innerPadding.calculateBottomPadding() + Spacing.lg,
            ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(apps, key = { it.id }) { app ->
                AppTile(app)
            }
        }
    }
}

@Composable
private fun AppTile(app: MiniApp) {
    Surface(
        onClick = app.onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                imageVector = app.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Sizes.appTileIcon),
            )
            Text(text = app.label, style = MaterialTheme.typography.titleMedium)
        }
    }
}
