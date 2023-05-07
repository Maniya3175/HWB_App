package com.example.hwbapp.data

import com.example.hwbapp.Utils.Resource
import kotlinx.coroutines.flow.MutableSharedFlow


interface HeightAndWeightReceiveManager {
    val data: MutableSharedFlow<Resource<HeightWeightResult>>

    fun reconnect()

    fun disconnect()

    fun startReceiving()

    fun closeConnection()

}