package com.aura.defense.security

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BTDevice(
        val name: String,
            val address: String,
                val type: String,
                    val bondState: String,
                        val isKnown: Boolean
)

data class BluetoothScanResult(
        val isAvailable: Boolean,
            val isEnabled: Boolean,
                val pairedDevices: List<BTDevice>,
                    val warnings: List<String>,
                        val timestamp: String
)

class BluetoothScanner(private val context: Context) {

        fun scan(): BluetoothScanResult {
                    val ts = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                            val adapter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            val mgr = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                                                        mgr?.adapter
                            } else {
                                            BluetoothAdapter.getDefaultAdapter()
                            }

                                    if (adapter == null) return BluetoothScanResult(false, false, emptyList(), listOf("Bluetooth no disponible"), ts)
                                            if (!adapter.isEnabled) return BluetoothScanResult(true, false, emptyList(), listOf("Bluetooth desactivado"), ts)

                                                    val devices = runCatching {
                                                                    adapter.bondedDevices.map { device ->
                                                                                    val bond = when (device.bondState) {
                                                                                                            BluetoothDevice.BOND_BONDED -> "Vinculado"
                                                                                                                                BluetoothDevice.BOND_BONDING -> "Vinculando"
                                                                                                                                                    else -> "No vinculado"
                                                                                    }
                                                                                                    val type = when (device.type) {
                                                                                                                            BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Clasico"
                                                                                                                                                BluetoothDevice.DEVICE_TYPE_LE -> "Low Energy"
                                                                                                                                                                    BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual"
                                                                                                                                                                                        else -> "Desconocido"
                                                                                                    }
                                                                                                                    BTDevice(device.name ?: "Sin nombre", device.address, type, bond, device.bondState == BluetoothDevice.BOND_BONDED)
                                                                    }.sortedByDescending { it.isKnown }
                                                    }.onFailure { Timber.e(it, "BT scan failed") }.getOrDefault(emptyList())

                                                            return BluetoothScanResult(true, true, devices, emptyList(), ts)
        }
}
