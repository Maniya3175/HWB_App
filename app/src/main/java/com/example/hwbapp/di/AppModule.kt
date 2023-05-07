package com.example.hwbapp.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.example.hwbapp.data.BicepReceiveManager
import com.example.hwbapp.data.HeightAndWeightReceiveManager
import com.example.hwbapp.data.ble.BicepBLEReceiveManager
import com.example.hwbapp.data.ble.HeightAndWeightBLEReceiveManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBluetoothAdapter(@ApplicationContext context: Context): BluetoothAdapter {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return manager.adapter
    }

    @Provides
    @Singleton
    fun provideHeightWeightReceiveManager(
        @ApplicationContext context: Context,
        bluetoothAdapter: BluetoothAdapter
    ): HeightAndWeightReceiveManager {
        return HeightAndWeightBLEReceiveManager(bluetoothAdapter,context)
    }

    @Provides
    @Singleton
    fun provideBicepReceiveManager(
        @ApplicationContext context: Context,
        bluetoothAdapter: BluetoothAdapter
    ): BicepReceiveManager {
        return BicepBLEReceiveManager(bluetoothAdapter,context)
    }
}