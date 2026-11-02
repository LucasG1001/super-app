package tech.gomeslab.superapp.feature.workout.ui.measure

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.gomeslab.superapp.core.navigation.Routes
import tech.gomeslab.superapp.core.ui.components.ChartPoint
import tech.gomeslab.superapp.core.ui.components.DatePickerDialogOneUi
import tech.gomeslab.superapp.core.ui.components.EditorScaffold
import tech.gomeslab.superapp.core.ui.components.ExtendedScaffold
import tech.gomeslab.superapp.core.ui.components.FocusCard
import tech.gomeslab.superapp.core.ui.components.LineChart
import tech.gomeslab.superapp.core.ui.components.OptionRow
import tech.gomeslab.superapp.core.ui.components.PlainTextField
import tech.gomeslab.superapp.core.ui.components.SectionHeader
import tech.gomeslab.superapp.core.ui.theme.Spacing
import tech.gomeslab.superapp.feature.workout.data.WorkoutRepository
import tech.gomeslab.superapp.feature.workout.domain.BodyMeasurement
import tech.gomeslab.superapp.feature.workout.domain.Circumference
import tech.gomeslab.superapp.feature.workout.domain.formatDecimal
import tech.gomeslab.superapp.feature.workout.domain.parseDecimal
import tech.gomeslab.superapp.feature.workout.ui.dayMonth
import tech.gomeslab.superapp.feature.workout.ui.relativeDay
import tech.gomeslab.superapp.feature.workout.ui.weightLabel
import java.time.LocalDate
import javax.inject.Inject

private const val CHART_DAYS = 90L

@HiltViewModel
class MeasurementsViewModel @Inject constructor(repository: WorkoutRepository) : ViewModel() {
    val measurements: StateFlow<List<BodyMeasurement>> =
        repository.measurements.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

data class MeasurementEditState(
    val date: LocalDate = LocalDate.now(),
    val weight: String = "",
    val bodyFat: String = "",
    val circumferences: Map<Circumference, String> = emptyMap(),
    val exists: Boolean = false,
) {
    val canSave: Boolean get() = weight.isNotBlank() || bodyFat.isNotBlank() || circumferences.values.any { it.isNotBlank() }
}

@HiltViewModel
class MeasurementEditViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(MeasurementEditState())
    val state: StateFlow<MeasurementEditState> = _state.asStateFlow()

    init {
        val epochDay = savedStateHandle.get<Long>(Routes.MEASUREMENT_EDIT_ARG) ?: -1L
        load(if (epochDay == -1L) LocalDate.now() else LocalDate.ofEpochDay(epochDay))
    }

    fun setDate(date: LocalDate) = load(date)
    fun setWeight(value: String) = _state.update { it.copy(weight = clean(value)) }
    fun setBodyFat(value: String) = _state.update { it.copy(bodyFat = clean(value)) }
    fun setCircumference(key: Circumference, value: String) =
        _state.update { it.copy(circumferences = it.circumferences + (key to clean(value))) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            repository.saveMeasurement(
                BodyMeasurement(
                    date = s.date,
                    weightKg = parseDecimal(s.weight),
                    bodyFatPct = parseDecimal(s.bodyFat),
                    circumferences = s.circumferences.mapNotNull { (k, v) -> parseDecimal(v)?.let { k to it } }.toMap(),
                ),
            )
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteMeasurement(_state.value.date)
            onDone()
        }
    }

    private fun load(date: LocalDate) {
        viewModelScope.launch {
            val existing = repository.getMeasurement(date)
            _state.value = MeasurementEditState(
                date = date,
                weight = existing?.weightKg?.let(::formatDecimal).orEmpty(),
                bodyFat = existing?.bodyFatPct?.let(::formatDecimal).orEmpty(),
                circumferences = existing?.circumferences?.mapValues { formatDecimal(it.value) }.orEmpty(),
                exists = existing != null,
            )
        }
    }

    private fun clean(value: String): String = value.filter { it.isDigit() || it == ',' || it == '.' }.take(6)
}

@Composable
fun MeasurementsScreen(
    onBack: () -> Unit,
    onEdit: (LocalDate?) -> Unit,
    viewModel: MeasurementsViewModel = hiltViewModel(),
) {
    val list by viewModel.measurements.collectAsStateWithLifecycle()
    val weights = list.filter { it.weightKg != null }
    val latest = weights.firstOrNull()
    val previous = weights.getOrNull(1)

    ExtendedScaffold(
        title = "Medidas",
        subtitle = latest?.weightKg?.let { "Peso atual: ${weightLabel(it)}" } ?: "Registre seu peso",
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
        },
        actions = {
            IconButton(onClick = { onEdit(null) }) { Icon(Icons.Filled.Add, contentDescription = "Registrar medidas") }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.md,
                end = Spacing.md,
                top = innerPadding.calculateTopPadding() + Spacing.xs,
                bottom = innerPadding.calculateBottomPadding() + Spacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (latest?.weightKg != null) {
                item(key = "current") {
                    FocusCard {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Text("Peso", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(weightLabel(latest.weightKg), style = MaterialTheme.typography.displayMedium)
                            val delta = previous?.weightKg?.let { latest.weightKg - it }
                            if (delta != null) {
                                Text(
                                    text = "${if (delta > 0) "+" else ""}${formatDecimal(delta)} kg desde ${relativeDay(previous.date).lowercase()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            val cutoff = LocalDate.now().minusDays(CHART_DAYS)
            val chart = weights
                .filter { !it.date.isBefore(cutoff) }
                .map { ChartPoint(it.date.toEpochDay().toDouble(), it.weightKg ?: 0.0, dayMonth(it.date)) }
            if (chart.size >= 2) {
                item(key = "chartHeader") { SectionHeader("Peso nos últimos 3 meses") }
                item(key = "chart") {
                    FocusCard {
                        LineChart(
                            points = chart,
                            formatValue = { "${formatDecimal(it)} kg" },
                            description = "Evolução do peso nos últimos 3 meses",
                            modifier = Modifier.padding(Spacing.md),
                        )
                    }
                }
            }
            item(key = "listHeader") { SectionHeader("Registros") }
            if (list.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = "Nenhum registro ainda. Toque em + para registrar peso e medidas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.md),
                    )
                }
            }
            items(list, key = { it.date.toEpochDay() }) { entry ->
                Surface(
                    onClick = { onEdit(entry.date) },
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(modifier = Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(relativeDay(entry.date), style = MaterialTheme.typography.titleMedium)
                            val details = listOfNotNull(
                                entry.bodyFatPct?.let { "${formatDecimal(it)}% gordura" },
                                entry.circumferences.size.takeIf { it > 0 }?.let { if (it == 1) "1 medida" else "$it medidas" },
                            ).joinToString(" · ")
                            if (details.isNotBlank()) {
                                Text(details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        entry.weightKg?.let { Text(weightLabel(it), style = MaterialTheme.typography.titleLarge) }
                    }
                }
            }
        }
    }
}

@Composable
fun MeasurementEditScreen(onDone: () -> Unit, viewModel: MeasurementEditViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var datePicker by rememberSaveable { mutableStateOf(false) }
    var showCircumferences by rememberSaveable { mutableStateOf(false) }

    EditorScaffold(
        title = "Medidas",
        canSave = state.canSave,
        onCancel = onDone,
        onSave = { viewModel.save(onDone) },
    ) {
        FocusCard {
            OptionRow(title = "Data", subtitle = relativeDay(state.date), onClick = { datePicker = true })
        }
        FocusCard {
            PlainTextField(value = state.weight, onValueChange = viewModel::setWeight, placeholder = "Peso (kg)", label = "Peso (kg)", keyboardType = KeyboardType.Decimal)
            PlainTextField(value = state.bodyFat, onValueChange = viewModel::setBodyFat, placeholder = "% de gordura", label = "% de gordura", keyboardType = KeyboardType.Decimal)
        }
        FocusCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OptionRow(
                    title = "Circunferências (cm)",
                    subtitle = "${state.circumferences.values.count { it.isNotBlank() }} de ${Circumference.entries.size} preenchidas",
                    onClick = { showCircumferences = !showCircumferences },
                    modifier = Modifier.weight(1f),
                    trailing = {
                        Icon(
                            if (showCircumferences) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                        )
                    },
                )
            }
            AnimatedVisibility(visible = showCircumferences) {
                Column {
                    Circumference.entries.forEach { key ->
                        PlainTextField(
                            value = state.circumferences[key].orEmpty(),
                            onValueChange = { viewModel.setCircumference(key, it) },
                            placeholder = key.label,
                            label = key.label,
                            keyboardType = KeyboardType.Decimal,
                        )
                    }
                }
            }
        }
        if (state.exists) {
            TextButton(onClick = { viewModel.delete(onDone) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Excluir registro deste dia", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (datePicker) {
        DatePickerDialogOneUi(
            initial = state.date,
            onConfirm = {
                datePicker = false
                viewModel.setDate(it)
            },
            onDismiss = { datePicker = false },
        )
    }
}
