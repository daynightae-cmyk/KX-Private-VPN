package com.knoux.kxprivatevpn.launch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    val power = context.getSystemService(PowerManager::class.java)
    val access = context.getSystemService(AccessibilityManager::class.java)
    fun read() = IntroTimeline.shouldReduceMotion(
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f),
        power.isPowerSaveMode, access.isTouchExplorationEnabled,
    )
    var reduced by remember { mutableStateOf(read()) }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { reduced = read() }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) { reduced = read() }
        }
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { reduced = read() }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer,
        )
        ContextCompat.registerReceiver(context, receiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        access.addTouchExplorationStateChangeListener(listener)
        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
            context.unregisterReceiver(receiver)
            access.removeTouchExplorationStateChangeListener(listener)
        }
    }
    return reduced
}
