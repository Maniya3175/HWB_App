package com.example.hwbapp.data

import com.example.hwbapp.Utils.Resource
import kotlinx.coroutines.flow.MutableSharedFlow

interface BicepReceiveManager {
    val data: MutableSharedFlow<Resource<BicepResult>>

    fun reconnect()

    fun disconnect()

    fun startReceiving()

    fun closeConnection()

}