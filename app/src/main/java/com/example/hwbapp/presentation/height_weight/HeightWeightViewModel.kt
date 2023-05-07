package com.example.hwbapp.presentation.height_weight

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hwbapp.Utils.Resource
import com.example.hwbapp.data.ConnectionState
import com.example.hwbapp.data.HeightAndWeightReceiveManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HeightWeightViewModel  @Inject constructor(
    private val heightAndWeightReceiveManager: HeightAndWeightReceiveManager
) : ViewModel(){

    var initializingMessage by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var height by mutableStateOf("000.00cm")
        private set

    var weight by mutableStateOf("00.000 Kg.")
        private set

    var connectionState by mutableStateOf<ConnectionState>(ConnectionState.Uninitialized)

    private fun subscribeToChanges(){
        viewModelScope.launch {
            heightAndWeightReceiveManager.data.collect{ result ->
                when(result){
                    is Resource.Success -> {
                        connectionState = result.data.connectionState
                        height = result.data.Height
                        weight = result.data.Weight
                    }

                    is Resource.Loading -> {
                        initializingMessage = result.message
                        connectionState = ConnectionState.CurrentlyInitializing
                    }

                    is Resource.Error -> {
                        errorMessage = result.errorMessage
                        connectionState = ConnectionState.Uninitialized
                    }
                }
            }
        }
    }

    fun disconnect(){
        heightAndWeightReceiveManager.disconnect()
    }

    fun reconnect(){
        heightAndWeightReceiveManager.reconnect()
    }

    fun initializeConnection(){
        errorMessage = null
        subscribeToChanges()
        heightAndWeightReceiveManager.startReceiving()
    }

    override fun onCleared() {
        super.onCleared()
        heightAndWeightReceiveManager.closeConnection()
    }
}