package tech.gomeslab.superapp.feature.alarm.domain

import tech.gomeslab.superapp.feature.alarm.data.AlarmEntity

fun AlarmEntity.toDomain(): Alarm = Alarm(
    id = id,
    hour = hour,
    minute = minute,
    label = label,
    enabled = enabled,
    daysOfWeek = daysOfWeek,
    soundUri = soundUri,
    vibrate = vibrate,
    snoozeMinutes = snoozeMinutes,
    gradualVolumeSeconds = gradualVolumeSeconds,
    createdAt = createdAt,
)

fun Alarm.toEntity(): AlarmEntity = AlarmEntity(
    id = id,
    hour = hour,
    minute = minute,
    label = label,
    enabled = enabled,
    daysOfWeek = daysOfWeek,
    soundUri = soundUri,
    vibrate = vibrate,
    snoozeMinutes = snoozeMinutes,
    gradualVolumeSeconds = gradualVolumeSeconds,
    createdAt = createdAt,
)
