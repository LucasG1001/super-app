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
}
