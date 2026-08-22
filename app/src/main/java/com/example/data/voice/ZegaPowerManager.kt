package com.example.data.voice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PowerState(
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val isSystemPowerSave: Boolean = false,
    val isEcoModeEnabled: Boolean = false,
    val isLowBattery: Boolean = false,
    val lowBatteryThreshold: Int = 20,
    val autoEcoModeEnabled: Boolean = true
)

class ZegaPowerManager(private val context: Context) {

    companion object {
        @Volatile
        private var instance: ZegaPowerManager? = null

        fun getInstance(context: Context): ZegaPowerManager {
            return instance ?: synchronized(this) {
                instance ?: ZegaPowerManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun isEcoModeActive(): Boolean = isEffectiveEcoMode

    fun setEcoMode(enabled: Boolean) = setManualEcoMode(enabled)

    private val prefs: SharedPreferences = context.getSharedPreferences("zega_power_prefs", Context.MODE_PRIVATE)

    private val _powerState = MutableStateFlow(PowerState())
    val powerState: StateFlow<PowerState> = _powerState.asStateFlow()

    private var onEcoModeChanged: ((Boolean) -> Unit)? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            updateBatteryInfo()
        }
    }

    init {
        val savedManualEco = prefs.getBoolean("manual_eco_mode", false)
        val savedAutoEco = prefs.getBoolean("auto_eco_mode", true)
        val savedThreshold = prefs.getInt("low_battery_threshold", 20)

        _powerState.value = _powerState.value.copy(
            isEcoModeEnabled = savedManualEco,
            autoEcoModeEnabled = savedAutoEco,
            lowBatteryThreshold = savedThreshold
        )

        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                }
            }
            context.registerReceiver(batteryReceiver, filter)
        } catch (e: Exception) {
            Log.e("ZegaPower", "Error registering battery receiver: ${e.message}")
        }

        updateBatteryInfo()
    }

    fun setOnEcoModeChangedListener(listener: (Boolean) -> Unit) {
        this.onEcoModeChanged = listener
    }

    fun updateBatteryInfo() {
        try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

            val level = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 80
            val isCharging = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                batteryManager?.isCharging == true
            } else {
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            }

            val isSystemPowerSave = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                powerManager?.isPowerSaveMode == true
            } else false

            val currentState = _powerState.value
            val isLowBattery = level <= currentState.lowBatteryThreshold && !isCharging

            // Determine effective eco mode: manual toggle OR (auto-eco enabled and (isLowBattery or system power save))
            val shouldBeEco = currentState.isEcoModeEnabled || (currentState.autoEcoModeEnabled && (isLowBattery || isSystemPowerSave))

            val newState = currentState.copy(
                batteryLevel = level,
                isCharging = isCharging,
                isSystemPowerSave = isSystemPowerSave,
                isLowBattery = isLowBattery
            )

            _powerState.value = newState
            onEcoModeChanged?.invoke(shouldBeEco)
        } catch (e: Exception) {
            Log.e("ZegaPower", "Error updating battery info: ${e.message}")
        }
    }

    fun setManualEcoMode(enabled: Boolean) {
        prefs.edit().putBoolean("manual_eco_mode", enabled).apply()
        val updated = _powerState.value.copy(isEcoModeEnabled = enabled)
        _powerState.value = updated
        val isEffectiveEco = enabled || (updated.autoEcoModeEnabled && (updated.isLowBattery || updated.isSystemPowerSave))
        onEcoModeChanged?.invoke(isEffectiveEco)
    }

    fun setAutoEcoMode(enabled: Boolean) {
        prefs.edit().putBoolean("auto_eco_mode", enabled).apply()
        val updated = _powerState.value.copy(autoEcoModeEnabled = enabled)
        _powerState.value = updated
        val isEffectiveEco = updated.isEcoModeEnabled || (enabled && (updated.isLowBattery || updated.isSystemPowerSave))
        onEcoModeChanged?.invoke(isEffectiveEco)
    }

    fun setLowBatteryThreshold(threshold: Int) {
        val clamped = threshold.coerceIn(10, 50)
        prefs.edit().putInt("low_battery_threshold", clamped).apply()
        _powerState.value = _powerState.value.copy(lowBatteryThreshold = clamped)
        updateBatteryInfo()
    }

    val isEffectiveEcoMode: Boolean
        get() {
            val s = _powerState.value
            return s.isEcoModeEnabled || (s.autoEcoModeEnabled && (s.isLowBattery || s.isSystemPowerSave))
        }

    fun cleanup() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // ignore
        }
    }
}
