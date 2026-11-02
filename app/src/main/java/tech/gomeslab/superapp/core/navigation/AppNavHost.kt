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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import tech.gomeslab.superapp.core.ui.home.HomeScreen
import tech.gomeslab.superapp.feature.alarm.ui.edit.AlarmEditScreen
import tech.gomeslab.superapp.feature.alarm.ui.list.AlarmListScreen
import tech.gomeslab.superapp.feature.reminder.ui.calendar.HolidayCalendarScreen
import tech.gomeslab.superapp.feature.reminder.ui.category.ReminderCategoryScreen
import tech.gomeslab.superapp.feature.reminder.ui.edit.ReminderEditScreen
import tech.gomeslab.superapp.feature.reminder.ui.home.ReminderHomeScreen
import tech.gomeslab.superapp.feature.workout.ui.active.ActiveWorkoutScreen
import tech.gomeslab.superapp.feature.workout.ui.exercise.ExerciseDetailScreen
import tech.gomeslab.superapp.feature.workout.ui.exercise.ExerciseEditScreen
import tech.gomeslab.superapp.feature.workout.ui.exercise.ExerciseListScreen
import tech.gomeslab.superapp.feature.workout.ui.history.HistoryScreen
import tech.gomeslab.superapp.feature.workout.ui.history.WorkoutDetailScreen
import tech.gomeslab.superapp.feature.workout.ui.home.WorkoutHomeScreen
import tech.gomeslab.superapp.feature.workout.ui.measure.MeasurementEditScreen
import tech.gomeslab.superapp.feature.workout.ui.measure.MeasurementsScreen
import tech.gomeslab.superapp.feature.workout.ui.routine.RoutineEditScreen

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
            HomeScreen(
                onOpenAlarm = { navController.navigate(Routes.ALARM_LIST) },
                onOpenReminders = { navController.navigate(Routes.REMINDER_HOME) },
                onOpenWorkout = { navController.navigate(Routes.WORKOUT_HOME) },
            )
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
        composable(Routes.REMINDER_HOME) {
            ReminderHomeScreen(
                onBack = { navController.popBackStack() },
                onAdd = { navController.navigate(Routes.reminderEdit()) },
                onOpenReminder = { id -> navController.navigate(Routes.reminderEdit(id)) },
                onOpenCategory = { category -> navController.navigate(Routes.reminderCategory(category.name)) },
                onOpenCalendar = { navController.navigate(Routes.HOLIDAY_CALENDAR) },
            )
        }
        composable(
            route = Routes.REMINDER_CATEGORY,
            arguments = listOf(navArgument(Routes.REMINDER_CATEGORY_ARG) { type = NavType.StringType }),
        ) {
            ReminderCategoryScreen(
                onBack = { navController.popBackStack() },
                onOpenReminder = { id -> navController.navigate(Routes.reminderEdit(id)) },
            )
        }
        composable(Routes.HOLIDAY_CALENDAR) {
            HolidayCalendarScreen(
                onBack = { navController.popBackStack() },
                onOpenReminder = { id -> navController.navigate(Routes.reminderEdit(id)) },
            )
        }
        composable(
            route = Routes.REMINDER_EDIT,
            arguments = listOf(
                navArgument(Routes.REMINDER_EDIT_ARG) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
            enterTransition = sheetEnter,
            exitTransition = pushExit,
            popEnterTransition = popEnter,
            popExitTransition = sheetExit,
        ) {
            ReminderEditScreen(onDone = { navController.popBackStack() })
        }
        workoutGraph(navController)
    }
}

private fun NavGraphBuilder.workoutGraph(navController: NavHostController) {
    composable(Routes.WORKOUT_HOME) {
        WorkoutHomeScreen(
            onBack = { navController.popBackStack() },
            onOpenActive = { navController.navigate(Routes.ACTIVE_WORKOUT) { launchSingleTop = true } },
            onEditRoutine = { id -> navController.navigate(Routes.routineEdit(id)) },
            onOpenHistory = { navController.navigate(Routes.WORKOUT_HISTORY) },
            onOpenExercises = { navController.navigate(Routes.exercises(pick = false)) },
            onOpenMeasurements = { navController.navigate(Routes.MEASUREMENTS) },
        )
    }
    composable(
        route = Routes.ROUTINE_EDIT,
        arguments = listOf(navArgument(Routes.ROUTINE_EDIT_ARG) { type = NavType.LongType; defaultValue = -1L }),
        enterTransition = sheetEnter,
        popExitTransition = sheetExit,
    ) { entry ->
        val picked by entry.savedStateHandle.getStateFlow<Long?>(Routes.RESULT_PICKED_EXERCISE, null).collectAsState()
        RoutineEditScreen(
            pickedExerciseId = picked,
            onPickedConsumed = { entry.savedStateHandle[Routes.RESULT_PICKED_EXERCISE] = null },
            onAddExercise = { navController.navigate(Routes.exercises(pick = true)) },
            onDone = { navController.popBackStack() },
        )
    }
    composable(Routes.ACTIVE_WORKOUT) { entry ->
        val picked by entry.savedStateHandle.getStateFlow<Long?>(Routes.RESULT_PICKED_EXERCISE, null).collectAsState()
        ActiveWorkoutScreen(
            pickedExerciseId = picked,
            onPickedConsumed = { entry.savedStateHandle[Routes.RESULT_PICKED_EXERCISE] = null },
            onAddExercise = { navController.navigate(Routes.exercises(pick = true)) },
            onMinimize = { navController.popBackStack() },
            onFinished = { id ->
                navController.navigate(Routes.workoutDetail(id, summary = true)) {
                    popUpTo(Routes.ACTIVE_WORKOUT) { inclusive = true }
                }
            },
            onDiscarded = { navController.popBackStack(Routes.ACTIVE_WORKOUT, inclusive = true) },
        )
    }
    composable(
        route = Routes.EXERCISES,
        arguments = listOf(navArgument(Routes.EXERCISES_PICK_ARG) { type = NavType.BoolType; defaultValue = false }),
    ) { entry ->
        val created by entry.savedStateHandle.getStateFlow<Long?>(Routes.RESULT_CREATED_EXERCISE, null).collectAsState()
        val pick = entry.arguments?.getBoolean(Routes.EXERCISES_PICK_ARG) ?: false
        val returnPicked: (Long) -> Unit = { id ->
            navController.previousBackStackEntry?.savedStateHandle?.set(Routes.RESULT_PICKED_EXERCISE, id)
            navController.popBackStack()
        }
        LaunchedEffect(created) {
            val id = created ?: return@LaunchedEffect
            entry.savedStateHandle[Routes.RESULT_CREATED_EXERCISE] = null
            if (pick) returnPicked(id)
        }
        ExerciseListScreen(
            onBack = { navController.popBackStack() },
            onPicked = returnPicked,
            onOpen = { id -> navController.navigate(Routes.exerciseDetail(id)) },
            onCreate = { name -> navController.navigate(Routes.exerciseEdit(name = name)) },
        )
    }
    composable(
        route = Routes.EXERCISE_EDIT,
        arguments = listOf(
            navArgument(Routes.EXERCISE_EDIT_ID_ARG) { type = NavType.LongType; defaultValue = -1L },
            navArgument(Routes.EXERCISE_EDIT_NAME_ARG) { type = NavType.StringType; defaultValue = "" },
        ),
        enterTransition = sheetEnter,
        popExitTransition = sheetExit,
    ) {
        ExerciseEditScreen(
            onDone = { navController.popBackStack() },
            onSaved = { id ->
                navController.previousBackStackEntry?.savedStateHandle?.set(Routes.RESULT_CREATED_EXERCISE, id)
                navController.popBackStack()
            },
        )
    }
    composable(
        route = Routes.EXERCISE_DETAIL,
        arguments = listOf(navArgument(Routes.EXERCISE_DETAIL_ARG) { type = NavType.LongType }),
    ) {
        ExerciseDetailScreen(
            onBack = { navController.popBackStack() },
            onEdit = { id -> navController.navigate(Routes.exerciseEdit(exerciseId = id)) },
        )
    }
    composable(Routes.WORKOUT_HISTORY) {
        HistoryScreen(
            onBack = { navController.popBackStack() },
            onOpen = { id -> navController.navigate(Routes.workoutDetail(id)) },
        )
    }
    composable(
        route = Routes.WORKOUT_DETAIL,
        arguments = listOf(
            navArgument(Routes.WORKOUT_DETAIL_ARG) { type = NavType.LongType },
            navArgument(Routes.WORKOUT_DETAIL_SUMMARY_ARG) { type = NavType.BoolType; defaultValue = false },
        ),
    ) {
        WorkoutDetailScreen(
            onBack = { navController.popBackStack() },
            onOpenExercise = { id -> navController.navigate(Routes.exerciseDetail(id)) },
        )
    }
    composable(Routes.MEASUREMENTS) {
        MeasurementsScreen(
            onBack = { navController.popBackStack() },
            onEdit = { date -> navController.navigate(Routes.measurementEdit(date?.toEpochDay())) },
        )
    }
    composable(
        route = Routes.MEASUREMENT_EDIT,
        arguments = listOf(navArgument(Routes.MEASUREMENT_EDIT_ARG) { type = NavType.LongType; defaultValue = -1L }),
        enterTransition = sheetEnter,
        popExitTransition = sheetExit,
    ) {
        MeasurementEditScreen(onDone = { navController.popBackStack() })
    }
}
