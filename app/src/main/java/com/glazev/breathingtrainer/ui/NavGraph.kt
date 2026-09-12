package com.glazev.breathingtrainer.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun NavGraph(
    viewModel: BreathingViewModel,
    onStartFromWidget: (onNavigateToTraining: () -> Unit) -> Unit = {}
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        onStartFromWidget {
            navController.navigate("training")
        }
    }

    NavHost(navController = navController, startDestination = "settings") {
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onStartClick = {
                    val activity = context as? Activity
                    if (activity != null) {
                        viewModel.startTrainingWithAd(activity) {
                            navController.navigate("training")
                            // Сразу запускаем логику тренировки (подготовительный отсчет)
                            viewModel.startTraining()
                        }
                    }
                }
            )
        }
        composable("training") {
            BreathingScreen(
                viewModel = viewModel,
                onBackClick = {
                    viewModel.stopTraining()
                    navController.popBackStack()
                }
            )
        }
    }
}
