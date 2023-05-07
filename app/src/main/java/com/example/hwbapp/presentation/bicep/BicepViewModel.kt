package com.example.hwbapp.presentation.bicep

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hwbapp.Utils.Resource
import com.example.hwbapp.data.BicepConnectionState
import com.example.hwbapp.data.BicepReceiveManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BicepViewModel @Inject constructor(
    private val BicepReceiveManager: BicepReceiveManager
) : ViewModel() {

    var initializingMessage by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var bicep by mutableStateOf("000.00cm")
        private set

    var bicepconnectionState by mutableStateOf<BicepConnectionState>(BicepConnectionState.Uninitialized)

    private fun subscribeToChanges(){
        viewModelScope.launch {
            BicepReceiveManager.data.collect{ result ->
                when(result){
                    is Resource.Success -> {
                        bicepconnectionState = result.data.connectionState
                        bicep = result.data.Bicep
                    }

                    is Resource.Loading -> {
                        initializingMessage = result.message
                        bicepconnectionState = BicepConnectionState.CurrentlyInitializing
                    }

                    is Resource.Error -> {
                        errorMessage = result.errorMessage
                        bicepconnectionState = BicepConnectionState.Uninitialized
                    }
                }
            }
        }
    }

    fun disconnect(){
        BicepReceiveManager.disconnect()
    }

    fun reconnect(){
        BicepReceiveManager.reconnect()
    }

    fun initializeConnection(){
        errorMessage = null
        subscribeToChanges()
        BicepReceiveManager.startReceiving()
    }

    override fun onCleared() {
        super.onCleared()
        BicepReceiveManager.closeConnection()
    }
}