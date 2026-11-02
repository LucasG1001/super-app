package tech.gomeslab.superapp.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import tech.gomeslab.superapp.core.ui.home.HomeScreen
import tech.gomeslab.superapp.feature.alarm.ui.edit.AlarmEditScreen
import tech.gomeslab.superapp.feature.alarm.ui.list.AlarmListScreen

private const val NAV_DURATION = 320

private typealias Enter = AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
private typealias Exit = AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition

private val pushEnter: Enter = {
    slideInHorizontally(tween(NAV_DURATION)) { it / 4 } + fadeIn(tween(NAV_DURATION))
}
private val pushExit: Exit = {
    slideOutHorizontally(tween(NAV_DURATION)) { -it / 8 } + fadeOut(tween(NAV_DURATION))
}
private val popEnter: Enter = {
    slideInHorizontally(tween(NAV_DURATION)) { -it / 8 } + fadeIn(tween(NAV_DURATION))
}
private val popExit: Exit = {
    slideOutHorizontally(tween(NAV_DURATION)) { it / 4 } + fadeOut(tween(NAV_DURATION))
}
private val sheetEnter: Enter = {
    slideInVertically(tween(NAV_DURATION)) { it / 6 } + fadeIn(tween(NAV_DURATION))
}
private val sheetExit: Exit = {
    slideOutVertically(tween(NAV_DURATION)) { it / 6 } + fadeOut(tween(NAV_DURATION))
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = pushEnter,
        exitTransition = pushExit,
        popEnterTransition = popEnter,
        popExitTransition = popExit,
    ) {
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
            enterTransition = sheetEnter,
            exitTransition = pushExit,
            popEnterTransition = popEnter,
            popExitTransition = sheetExit,
        ) {
            AlarmEditScreen(onDone = { navController.popBackStack() })
        }
    }
}
