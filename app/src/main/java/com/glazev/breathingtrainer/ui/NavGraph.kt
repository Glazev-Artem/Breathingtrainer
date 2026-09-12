package com.glazev.breathingtrainer.ui

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun NavGraph(viewModel: BreathingViewModel, startDestination: String = "settings") {
    val navController = rememberNavController()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { destination ->
            if (destination == "training") {
                if (navController.currentDestination?.route != "training") {
                    navController.navigate("training") {
                        popUpTo("settings") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = "settings") {
        composable("settings") {
            var handledStart by rememberSaveable { mutableStateOf(false) }
            
            LaunchedEffect(handledStart, startDestination) {
                if (!handledStart && startDestination == "training") {
                    navController.navigate("training") {
                        launchSingleTop = true
                    }
                    handledStart = true
                }
            }
            
            if (startDestination == "training" && !handledStart) {
                Box(modifier = Modifier.fillMaxSize())
            } else {
                SettingsScreen(
                    viewModel = viewModel,
                    onStartClick = {
                        val activity = context as? Activity
                        if (activity != null) {
                            viewModel.startTrainingWithAd(activity) {
                                navController.navigate("training")
                                viewModel.startTraining()
                            }
                        }
                    }
                )
            }
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
