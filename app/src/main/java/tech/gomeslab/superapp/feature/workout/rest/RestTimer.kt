package tech.gomeslab.superapp.feature.workout.rest

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class RestState(val endsAt: Long, val totalMillis: Long, val exerciseName: String)

object WorkoutNotifications {
    const val RUNNING_CHANNEL_ID = "workout_rest_running"
    const val DONE_CHANNEL_ID = "workout_rest_done"
    const val RUNNING_ID = 60_001
    const val DONE_ID = 60_002
    const val ACTION_REST_DONE = "tech.gomeslab.superapp.workout.REST_DONE"
    const val ACTION_REST_SKIP = "tech.gomeslab.superapp.workout.REST_SKIP"
    private const val DONE_TIMEOUT_MILLIS = 60_000L

    fun doneTimeout(): Long = DONE_TIMEOUT_MILLIS
}

/**
 * Descanso entre séries. A contagem aparece na tela do treino e numa notificação silenciosa com
 * cronômetro regressivo; um alarme exato no fim avisa "Descanso acabou" mesmo com a tela bloqueada.
 */
@Singleton
class RestTimer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _state = MutableStateFlow<RestState?>(null)
    val state: StateFlow<RestState?> = _state.asStateFlow()

    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val notifications = NotificationManagerCompat.from(context)

    fun start(seconds: Int, exerciseName: String) {
        if (seconds <= 0) return
        val total = seconds * 1_000L
        schedule(RestState(System.currentTimeMillis() + total, total, exerciseName))
    }

    fun adjust(deltaSeconds: Int) {
        val current = _state.value ?: return
        val now = System.currentTimeMillis()
        val endsAt = (current.endsAt + deltaSeconds * 1_000L).coerceAtLeast(now + 1_000L)
        schedule(current.copy(endsAt = endsAt, totalMillis = (current.totalMillis + deltaSeconds * 1_000L).coerceAtLeast(endsAt - now)))
    }

    fun skip() {
        alarmManager.cancel(donePendingIntent())
        notifications.cancel(WorkoutNotifications.RUNNING_ID)
        _state.value = null
    }

    internal fun onFinished() {
        val finished = _state.value
        _state.value = null
        notifications.cancel(WorkoutNotifications.RUNNING_ID)
        val builder = NotificationCompat.Builder(context, WorkoutNotifications.DONE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Descanso acabou")
            .setContentText(finished?.exerciseName?.let { "Próxima série: $it" } ?: "Hora da próxima série")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .setTimeoutAfter(WorkoutNotifications.doneTimeout())
        notify(WorkoutNotifications.DONE_ID, builder)
    }

    private fun schedule(state: RestState) {
        _state.value = state
        notifications.cancel(WorkoutNotifications.DONE_ID)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, state.endsAt, donePendingIntent())
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, state.endsAt, donePendingIntent())
        }
        showRunning(state)
    }

    private fun showRunning(state: RestState) {
        val skip = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, RestTimerReceiver::class.java).setAction(WorkoutNotifications.ACTION_REST_SKIP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, WorkoutNotifications.RUNNING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Descansando")
            .setContentText(state.exerciseName)
            .setWhen(state.endsAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openAppIntent())
            .addAction(0, "Pular descanso", skip)
        notify(WorkoutNotifications.RUNNING_ID, builder)
    }

    private fun notify(id: Int, builder: NotificationCompat.Builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        notifications.notify(id, builder.build())
    }

    private fun donePendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, RestTimerReceiver::class.java).setAction(WorkoutNotifications.ACTION_REST_DONE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        context.packageManager.getLaunchIntentForPackage(context.packageName),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

@AndroidEntryPoint
class RestTimerReceiver : BroadcastReceiver() {

    @Inject lateinit var restTimer: RestTimer

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WorkoutNotifications.ACTION_REST_DONE -> restTimer.onFinished()
            WorkoutNotifications.ACTION_REST_SKIP -> restTimer.skip()
        }
    }
}
