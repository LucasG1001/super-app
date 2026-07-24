package tech.gomeslab.superapp.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import tech.gomeslab.superapp.core.ui.home.HomeScreen
import tech.gomeslab.superapp.feature.alarm.ui.edit.AlarmEditScreen
import tech.gomeslab.superapp.feature.alarm.ui.list.AlarmListScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(onOpenAlarm = { navController.navigate(Routes.ALARM_LIST) })
        }
        composable(Routes.ALARM_LIST) {
            AlarmListScreen(
                onAddAlarm = { navController.navigate(Routes.alarmEdit()) },
                onEditAlarm = { id -> navController.navigate(Routes.alarmEdit(id)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.ALARM_EDIT,
            arguments = listOf(
                navArgument(Routes.ALARM_EDIT_ARG) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) {
            AlarmEditScreen(onDone = { navController.popBackStack() })
        }
    }
}
