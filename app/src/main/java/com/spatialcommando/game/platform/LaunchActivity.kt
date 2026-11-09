package com.spatialcommando.game.platform

import android.hardware.input.InputManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.pico.spatial.ui.platform.stub.SpatialLaunchActivity
import com.spatialcommando.game.game.GamepadInputCache

class LaunchActivity : SpatialLaunchActivity(), InputManager.InputDeviceListener {
    private var inputManager: InputManager? = null
    private var reportedAxes = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inputManager = getSystemService(InputManager::class.java)
        inputManager?.registerInputDeviceListener(this, Handler(Looper.getMainLooper()))
    }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val mask = when(event.keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> 1
            KeyEvent.KEYCODE_BUTTON_B -> 16
            KeyEvent.KEYCODE_BUTTON_X -> 4
            KeyEvent.KEYCODE_BUTTON_Y -> 8
            KeyEvent.KEYCODE_BUTTON_R1 -> 2
            KeyEvent.KEYCODE_BUTTON_R2 -> 128
            KeyEvent.KEYCODE_BUTTON_START -> 32
            KeyEvent.KEYCODE_BUTTON_SELECT -> 64
            else -> 0
        }
        if(mask != 0 && (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP)) {
            GamepadInputCache.setButton(mask,event.action == KeyEvent.ACTION_DOWN,event.deviceId,SystemClock.elapsedRealtime())
            if(event.repeatCount == 0) Log.i("IronFront", "Android gamepad key=${event.keyCode} action=${event.action} device=${event.deviceId}")
            return true
        }
        return super.dispatchKeyEvent(event)
    }
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if(event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK && event.action == MotionEvent.ACTION_MOVE) {
            val x = event.getAxisValue(MotionEvent.AXIS_X)
            val y = -event.getAxisValue(MotionEvent.AXIS_Y)
            val rightY = -event.getAxisValue(if(event.device?.getMotionRange(MotionEvent.AXIS_RZ)!=null)MotionEvent.AXIS_RZ else MotionEvent.AXIS_RY)
            val trigger = maxOf(event.getAxisValue(MotionEvent.AXIS_RTRIGGER), event.getAxisValue(MotionEvent.AXIS_GAS))
            GamepadInputCache.setAxes(x,y,trigger,event.deviceId,SystemClock.elapsedRealtime(),rightY)
            if(!reportedAxes) { reportedAxes = true; Log.i("IronFront", "Android joystick input active device=${event.deviceId}") }
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }
    override fun onInputDeviceAdded(deviceId: Int) {}
    override fun onInputDeviceChanged(deviceId: Int) {}
    override fun onInputDeviceRemoved(deviceId: Int) { GamepadInputCache.clearDevice(deviceId,SystemClock.elapsedRealtime()) }
    override fun onPause() { GamepadInputCache.clear(); super.onPause() }
    override fun onDestroy() { inputManager?.unregisterInputDeviceListener(this); inputManager=null; super.onDestroy() }
}
