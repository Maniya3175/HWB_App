package com.example.hwbapp.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.example.hwbapp.Utils.Resource
import com.example.hwbapp.data.ConnectionState
import com.example.hwbapp.data.HeightAndWeightReceiveManager
import com.example.hwbapp.data.HeightWeightResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject


@SuppressLint("MissingPermission")
class HeightAndWeightBLEReceiveManager @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter,
    private val context: Context
) : HeightAndWeightReceiveManager{

    override val data: MutableSharedFlow<Resource<HeightWeightResult>> = MutableSharedFlow()

    private val myHeight = mutableStateOf("H")
    private val myWeight = mutableStateOf("W")

    private val DEVICE_NAME = "ESP32_M"
    private val HEIGHT_WEIGHT_SERVICE_UIID = "f9028070-6fd4-423f-ae93-bbc4dabbc0ff"
    private val HEIGHT_CHARACTERISTICS_UUID = "6b8d13b9-9602-47b6-833a-31bdf86cdf86"
    private val WEIGHT_CHARACTERISTICS_UUID =  "1b62e587-d1ee-4d4f-8526-6aa5f0af4f85"

    private val DEVICE_NAME1 = "ES-Tape"
    private val ESTAPE_SERVICE_UIID = "0783b03e-8535-b5a0-7140-a304d2495cb7"
    private val ESTAPE_CHARACTERISTICS_UUID = "0783b03e-8535-b5a0-7140-a304d2495cb8"

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanFilter = ScanFilter.Builder()
        .setDeviceName(DEVICE_NAME)
        .build()

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private var gatt: BluetoothGatt? = null

    private var isScanning = false

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val scanCallback = object : ScanCallback(){

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if(result.device.name == DEVICE_NAME){
                coroutineScope.launch {
                    data.emit(Resource.Loading(message = "Connecting to device..."))
                }
                if(isScanning){
                    result.device.connectGatt(context,false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                    isScanning = false
                    bleScanner.stopScan(this)
                }
            }
        }
    }


    private var currentConnectionAttempt = 1
    private var MAXIMUM_CONNECTION_ATTEMPTS = 5

    private val gattCallback = object : BluetoothGattCallback(){
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if(status == BluetoothGatt.GATT_SUCCESS){
                if(newState == BluetoothProfile.STATE_CONNECTED){
                    coroutineScope.launch {
                        data.emit(Resource.Loading(message = "Discovering Services..."))
                    }
                    gatt.discoverServices()
                    this@HeightAndWeightBLEReceiveManager.gatt = gatt
                } else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                    coroutineScope.launch {
                        data.emit(Resource.Success(data = HeightWeightResult("000.00cm","00.000 Kg.", ConnectionState.Disconnected)))
                    }
                    gatt.close()
                }
            }else{
                gatt.close()
                currentConnectionAttempt+=1
                coroutineScope.launch {
                    data.emit(
                        Resource.Loading(
                            message = "Attempting to connect $currentConnectionAttempt/$MAXIMUM_CONNECTION_ATTEMPTS"
                        )
                    )
                }
                if(currentConnectionAttempt<=MAXIMUM_CONNECTION_ATTEMPTS){
                    startReceiving()
                }else{
                    coroutineScope.launch {
                        data.emit(Resource.Error(errorMessage = "Could not connect to ble device"))
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt){
                printGattTable()
                coroutineScope.launch {
                    data.emit(Resource.Loading(message = "Adjusting MTU space..."))
                }
                gatt.requestMtu(517)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            coroutineScope.launch {
                val characteristic = findCharacteristics(HEIGHT_WEIGHT_SERVICE_UIID, HEIGHT_CHARACTERISTICS_UUID)
                if(characteristic == null){
                    coroutineScope.launch {
                        data.emit(Resource.Error(errorMessage = "Could not find Height and Weight publisher"))
                    }
                    return@launch
                }
                enableNotification(characteristic)
            }

            coroutineScope.launch(){
                delay(100)
                val characteristic1 = findCharacteristics(HEIGHT_WEIGHT_SERVICE_UIID, WEIGHT_CHARACTERISTICS_UUID)
                if(characteristic1 == null){
                    coroutineScope.launch {
                        data.emit(Resource.Error(errorMessage = "Could not find Height and Weight publisher"))
                    }
                    return@launch
                }
                enableNotification(characteristic1)
            }
        }


        // Working Part
//        override fun onCharacteristicChanged(
//            gatt: BluetoothGatt,
//            characteristic: BluetoothGattCharacteristic
//        ) {
//            with(characteristic){
//                when(uuid){
//                    UUID.fromString(HEIGHT_CHARACTERISTICS_UUID) -> {
//                        val Height = characteristic?.value!!.decodeToString()
//                        val Weight = characteristic?.value!!.decodeToString()
//                        val tempHumidityResult = HeightWeightResult(Height, Weight, ConnectionState.Connected)
//                        coroutineScope.launch {
//                            data.emit(
//                                Resource.Success(data = tempHumidityResult)
//                            )
//                        }
//                    }
//                    UUID.fromString(WEIGHT_CHARACTERISTICS_UUID) -> {
//                        val Height = characteristic?.value!!.decodeToString()
//                        val Weight = characteristic?.value!!.decodeToString()
//                        val heightWeightResult = HeightWeightResult(Height, Weight, ConnectionState.Connected)
//                        coroutineScope.launch {
//                            data.emit(
//                                Resource.Success(data = heightWeightResult)
//                            )
//                        }
//                    }
//                    else -> Unit
//                }
//            }
//        }

        //Working Part
//        override fun onCharacteristicChanged(
//            gatt: BluetoothGatt,
//            characteristic: BluetoothGattCharacteristic
//        ) {
//
//            val heightWeightResult = when (characteristic.uuid) {
//                UUID.fromString(HEIGHT_CHARACTERISTICS_UUID) -> {
//                    val height = characteristic.value!!.decodeToString()
//                    val weight = characteristic.value!!.decodeToString()
//                    HeightWeightResult(height, weight, ConnectionState.Connected)
//                }
//                UUID.fromString(WEIGHT_CHARACTERISTICS_UUID) -> {
//                    val height = characteristic.value!!.decodeToString()
//                    val weight = characteristic.value!!.decodeToString()
//                    HeightWeightResult(height, weight, ConnectionState.Connected)
//                }
//                else -> null
//            }
//
//            heightWeightResult?.let {
//                coroutineScope.launch {
//                    data.emit(
//                        Resource.Success(data = it)
//                    )
//                }
//            }
//        }

        //Working
//        override fun onCharacteristicChanged(
//            gatt: BluetoothGatt,
//            characteristic: BluetoothGattCharacteristic
//        ) {
//            val height = when (characteristic.uuid) {
//                UUID.fromString(HEIGHT_CHARACTERISTICS_UUID) -> characteristic?.value!!.decodeToString()
//                else -> "H"
//            }
//
//            val weight = when (characteristic.uuid) {
//                UUID.fromString(WEIGHT_CHARACTERISTICS_UUID) -> characteristic?.value!!.decodeToString()
//                else -> "W"
//            }
//
//
//            val heightWeightResult = HeightWeightResult(height, weight, ConnectionState.Connected)
//            heightWeightResult?.let {
//                coroutineScope.launch {
//                    data.emit(
//                        Resource.Success(data = it)
//                    )
//                }
//            }
//        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            when (characteristic.uuid) {
                UUID.fromString(HEIGHT_CHARACTERISTICS_UUID) -> myHeight.value = characteristic?.value!!.decodeToString()
                UUID.fromString(WEIGHT_CHARACTERISTICS_UUID) -> myWeight.value = characteristic?.value!!.decodeToString()
                else -> null
            }

            val heightWeightResult = HeightWeightResult(myHeight.value, myWeight.value, ConnectionState.Connected)
            heightWeightResult?.let {
                coroutineScope.launch {
                    data.emit(
                        Resource.Success(data = it)
                    )
                }
            }
        }



//        override fun onCharacteristicChanged(
//            gatt: BluetoothGatt,
//            characteristic: BluetoothGattCharacteristic
//        ) {
//            with(characteristic){
////                var Height: String = "H"
////                var Weight: String = "W"
//                with(characteristic){
//                    when(uuid){
//                        UUID.fromString(HEIGHT_CHARACTERISTICS_UUID) -> {
//                            val height = characteristic?.value!!.decodeToString()
//                            Height = height
//                        }
//                        UUID.fromString(WEIGHT_CHARACTERISTICS_UUID) -> {
//                            val weight = characteristic?.value!!.decodeToString()
//                            Weight = weight
//                        }
//                        else -> Unit
//                    }
//                    val heightWeightResult = HeightWeightResult(Height, Weight, ConnectionState.Connected)
//                    coroutineScope.launch {
//                        data.emit(
//                            Resource.Success(data = heightWeightResult)
//                        )
//                    }
//                }
//
//
//            }
//        }

//        override fun onCharacteristicChanged(
//            gatt: BluetoothGatt,
//            characteristic: BluetoothGattCharacteristic
//        ) {
//            var height1: String = "H"
//            var weight1: String = "W"
//
//            coroutineScope.launch {
//                with(characteristic){
//                    if (uuid == UUID.fromString(HEIGHT_CHARACTERISTICS_UUID)){
//                        characteristic?.value!!.decodeToString().also { height1 = it }
//                    }else if (uuid == UUID.fromString(WEIGHT_CHARACTERISTICS_UUID)){
//                        characteristic?.value!!.decodeToString().also { weight1 = it }
//                    }
//                }
//            }.also {
//                coroutineScope.launch {
//                    val heightWeightResult = HeightWeightResult(height1, weight1, ConnectionState.Connected)
//                    data.emit(
//                        Resource.Success(data = heightWeightResult)
//                    )
//                }
//            }
//
//        }


    }

    private fun enableNotification(characteristic: BluetoothGattCharacteristic){
        val cccdUuid = UUID.fromString(CCCD_DESCRIPTOR_UUID)
        val payload = when {
            characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> return
        }

        characteristic.getDescriptor(cccdUuid)?.let { cccdDescriptor ->
            if(gatt?.setCharacteristicNotification(characteristic, true) == false){
                Log.d("BLEReceiveManager","set characteristics notification failed")
                return
            }
            writeDescription(cccdDescriptor, payload)
        }
    }

    private fun writeDescription(descriptor: BluetoothGattDescriptor, payload: ByteArray){
        gatt?.let { gatt ->
            descriptor.value = payload
            gatt.writeDescriptor(descriptor)
        } ?: error("Not connected to a BLE device!")
    }

    private fun findCharacteristics(serviceUUID: String, characteristicsUUID:String):BluetoothGattCharacteristic?{
        return gatt?.services?.find { service ->
            service.uuid.toString() == serviceUUID
        }?.characteristics?.find { characteristics ->
            characteristics.uuid.toString() == characteristicsUUID
        }
    }

    override fun startReceiving() {
        coroutineScope.launch {
            data.emit(Resource.Loading(message = "Scanning Ble devices..."))
        }
        isScanning = true
        bleScanner.startScan(mutableListOf(scanFilter),scanSettings,scanCallback)
    }


    override fun reconnect() {
        gatt?.connect()
    }

    override fun disconnect() {
        gatt?.disconnect()
    }

    override fun closeConnection() {
        bleScanner.stopScan(scanCallback)
        val characteristic = findCharacteristics(HEIGHT_WEIGHT_SERVICE_UIID, HEIGHT_CHARACTERISTICS_UUID)
        val characteristic1 = findCharacteristics(HEIGHT_WEIGHT_SERVICE_UIID, WEIGHT_CHARACTERISTICS_UUID)
        if(characteristic != null){
           disconnectCharacteristic(characteristic)
        }
        if(characteristic1 != null){
            disconnectCharacteristic(characteristic1)
        }
        gatt?.close()
    }

    private fun disconnectCharacteristic(characteristic: BluetoothGattCharacteristic){
        val cccdUuid = UUID.fromString(CCCD_DESCRIPTOR_UUID)
        characteristic.getDescriptor(cccdUuid)?.let { cccdDescriptor ->
            if(gatt?.setCharacteristicNotification(characteristic,false) == false){
                Log.d("HeightWeightReceiveManager","set charateristics notification failed")
                return
            }
            writeDescription(cccdDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
        }
    }

}