package tech.gomeslab.superapp.feature.alarm.domain

import java.util.Calendar

object NextAlarm {

    fun nextTriggerAt(now: Calendar, alarms: List<Alarm>): Long? =
        alarms
            .filter { it.enabled }
            .minOfOrNull { NextTrigger.compute(now, it.hour, it.minute, it.daysOfWeek) }
}
