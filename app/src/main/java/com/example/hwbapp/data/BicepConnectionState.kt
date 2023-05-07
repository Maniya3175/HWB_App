package com.example.hwbapp.data

sealed interface BicepConnectionState{
    object Connected: BicepConnectionState
    object Disconnected: BicepConnectionState
    object Uninitialized: BicepConnectionState
    object CurrentlyInitializing: BicepConnectionState
}