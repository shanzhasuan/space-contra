package com.spatialcommando.game.game

import java.util.concurrent.atomic.AtomicReference

/** Standard Android gamepad fallback. Native tracking takes priority whenever it is fresh. */
object GamepadInputCache {
    private data class State(val x: Float = 0f, val y: Float = 0f, val trigger: Float = 0f, val rightY: Float = 0f, val buttons: Int = 0, val devices: Set<Int> = emptySet())
    private val state = AtomicReference<State?>(null)
    fun setAxes(x: Float, y: Float, trigger: Float, deviceId: Int, now: Long, rightY: Float = 0f) {
        state.getAndUpdate { old -> (old ?: State()).let { it.copy(x=x, y=y, trigger=trigger, rightY=rightY, devices=it.devices + deviceId) } }
        publish(now)
    }
    fun setButton(mask: Int, pressed: Boolean, deviceId: Int, now: Long) {
        state.getAndUpdate { old -> (old ?: State()).let { it.copy(buttons=if(pressed) it.buttons or mask else it.buttons and mask.inv(), devices=it.devices + deviceId) } }
        publish(now)
    }
    fun publish(now: Long) {
        val nativeAt = ControllerInputCache.lastNativeReceivedAt.get()
        if(nativeAt > 0L && now - nativeAt < 1000L) return
        val pad = state.get() ?: return
        val b = pad.buttons
        ControllerInputCache.submit(InputFrame(x=pad.x,y=pad.y,
            jump=b and 1 != 0, shoot=b and (2 or 16 or 128) != 0 || pad.trigger > 0.5f,
            switch=b and 4 != 0, pause=b and 32 != 0, back=b and 64 != 0, prone=pad.rightY < -0.55f),now)
    }
    fun clear() { state.set(null); ControllerInputCache.clear() }
    fun clearDevice(deviceId: Int, now: Long) {
        if(state.get()?.devices?.contains(deviceId) != true) return
        state.set(null)
        val nativeAt = ControllerInputCache.lastNativeReceivedAt.get()
        if(nativeAt == 0L || now - nativeAt >= 1000L) ControllerInputCache.clear()
    }
}
