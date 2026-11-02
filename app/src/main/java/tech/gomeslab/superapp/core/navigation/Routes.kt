package tech.gomeslab.superapp.core.navigation

// Rotas do app. Cada feature ("app" do super app) expõe seu próprio conjunto.
object Routes {
    const val HOME = "home"
    const val ALARM_LIST = "alarm/list"

    // arg opcional: id do alarme a editar (ausente = novo).
    const val ALARM_EDIT = "alarm/edit?alarmId={alarmId}"
    const val ALARM_EDIT_ARG = "alarmId"

    fun alarmEdit(alarmId: Long? = null): String =
        if (alarmId == null) "alarm/edit" else "alarm/edit?alarmId=$alarmId"

    const val REMINDER_HOME = "reminder/home"
    const val HOLIDAY_CALENDAR = "reminder/calendar"

    const val REMINDER_CATEGORY = "reminder/category/{category}"
    const val REMINDER_CATEGORY_ARG = "category"

    fun reminderCategory(category: String): String = "reminder/category/$category"

    const val REMINDER_EDIT = "reminder/edit?reminderId={reminderId}"
    const val REMINDER_EDIT_ARG = "reminderId"

    fun reminderEdit(reminderId: Long? = null): String =
        if (reminderId == null) "reminder/edit" else "reminder/edit?reminderId=$reminderId"

    const val WORKOUT_HOME = "workout/home"
    const val ACTIVE_WORKOUT = "workout/active"
    const val WORKOUT_HISTORY = "workout/history"
    const val MEASUREMENTS = "workout/measurements"

    const val ROUTINE_EDIT = "workout/routine?routineId={routineId}"
    const val ROUTINE_EDIT_ARG = "routineId"

    fun routineEdit(routineId: Long? = null): String =
        if (routineId == null) "workout/routine" else "workout/routine?routineId=$routineId"

    const val EXERCISES = "workout/exercises?pick={pick}"
    const val EXERCISES_PICK_ARG = "pick"

    fun exercises(pick: Boolean): String = "workout/exercises?pick=$pick"

    const val EXERCISE_EDIT = "workout/exercise/edit?exerciseId={exerciseId}&name={name}"
    const val EXERCISE_EDIT_ID_ARG = "exerciseId"
    const val EXERCISE_EDIT_NAME_ARG = "name"

    fun exerciseEdit(exerciseId: Long? = null, name: String = ""): String =
        "workout/exercise/edit?exerciseId=${exerciseId ?: -1}&name=${android.net.Uri.encode(name)}"

    const val EXERCISE_DETAIL = "workout/exercise/{exerciseId}"
    const val EXERCISE_DETAIL_ARG = "exerciseId"

    fun exerciseDetail(exerciseId: Long): String = "workout/exercise/$exerciseId"

    const val WORKOUT_DETAIL = "workout/detail/{workoutId}?summary={summary}"
    const val WORKOUT_DETAIL_ARG = "workoutId"
    const val WORKOUT_DETAIL_SUMMARY_ARG = "summary"

    fun workoutDetail(workoutId: Long, summary: Boolean = false): String = "workout/detail/$workoutId?summary=$summary"

    const val MEASUREMENT_EDIT = "workout/measurement?epochDay={epochDay}"
    const val MEASUREMENT_EDIT_ARG = "epochDay"

    fun measurementEdit(epochDay: Long? = null): String =
        if (epochDay == null) "workout/measurement" else "workout/measurement?epochDay=$epochDay"

    /** Chaves de resultado entre telas (SavedStateHandle da entrada anterior). */
    const val RESULT_PICKED_EXERCISE = "picked_exercise"
    const val RESULT_CREATED_EXERCISE = "created_exercise"
}
