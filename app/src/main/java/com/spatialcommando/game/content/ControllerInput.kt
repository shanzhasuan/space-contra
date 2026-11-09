package com.spatialcommando.game.content

import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pico.spatial.tracking.controller.ControllerTrackingProvider
import com.spatialcommando.game.game.ControllerInputCache
import com.spatialcommando.game.game.ControllerSnapshot
import com.spatialcommando.game.game.GameRuntime
import com.spatialcommando.game.game.InputFrame

@Composable
fun ControllerInputLifecycle(enabled: Boolean) {
    val provider = remember { ControllerTrackingProvider() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(provider, lifecycle, enabled) {
        val listener = ControllerTrackingProvider.ControllerActionListener { action ->
            // The callback is on the tracking data-source thread. No Entity or Compose writes here.
            ControllerInputCache.submitNative(
                InputFrame(x=action.left.thumbstickValue.x, y=action.left.thumbstickValue.y,
                    jump=action.right.aButtonPressed,
                    shoot=action.right.bButtonPressed || action.right.triggerPressed,
                    switch=action.left.xButtonPressed,
                    prone=action.right.thumbstickValue.y < -0.55f), SystemClock.elapsedRealtime())
        }
        fun startProvider() { if (enabled) { val result=provider.start(); if(provider.supportState.toString()=="DEVICE_NOT_SUPPORTED")GameRuntime.controlText="暂时无法读取手柄输入"; Log.i("IronFront", "Controller start=$result support=${provider.supportState}") } }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> startProvider()
                Lifecycle.Event.ON_PAUSE -> { provider.stop(); ControllerInputCache.clear(); GameRuntime.pause() }
                else -> Unit
            }
        }
        provider.addControllerActionListener(listener)
        lifecycle.addObserver(observer)
        startProvider()
        onDispose {
            lifecycle.removeObserver(observer); provider.removeControllerActionListener(listener)
            provider.stop(); ControllerInputCache.clear()
        }
    }
}
