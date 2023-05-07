package com.example.hwbapp.presentation.ble_start

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.hwbapp.presentation.HeightWeightScreen
import com.example.hwbapp.presentation.StartScreen
import com.example.hwbapp.presentation.bicep.BicepScreen

@Composable
fun Navigation(
    onBluetoothStateChanged:()->Unit
) {

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.StartScreen.route){
        composable(Screen.StartScreen.route){
            StartScreen(navController = navController)
        }

        composable(Screen.HeightWeightScreen.route){
            HeightWeightScreen(
                navController = navController,
                onBluetoothStateChanged = onBluetoothStateChanged
            )
        }

        composable(Screen.BicepScreen.route){
            BicepScreen(
                navController = navController,
                onBluetoothStateChanged = onBluetoothStateChanged
            )
        }
    }

}

sealed class Screen(val route:String){
    object StartScreen:Screen("start_screen")
    object HeightWeightScreen:Screen("height_Weight_screen")
    object BicepScreen:Screen("Bicep_screen")
}