package com.spatialcommando.game

import com.spatialcommando.game.game.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GamepadInputTest {
    @Before fun reset() { GamepadInputCache.clear() }
    @Test fun shortGamepadTapSurvivesUntilSceneConsumesIt() {
        GamepadInputCache.setButton(1,true,20,100)
        GamepadInputCache.setButton(1,false,20,110)
        assertTrue(ControllerInputCache.consume()!!.frame.jump)
        assertFalse(ControllerInputCache.consume()!!.frame.jump)
    }
    @Test fun freshNativeTrackingHasPriorityOverGamepadFallback() {
        ControllerInputCache.submitNative(InputFrame(x=-1f),100)
        GamepadInputCache.setAxes(1f,0f,0f,20,200)
        assertEquals(-1f,ControllerInputCache.latest.get()!!.frame.x,0f)
        GamepadInputCache.publish(1200)
        assertEquals(1f,ControllerInputCache.latest.get()!!.frame.x,0f)
    }
    @Test fun removingEitherControllerClearsHeldMovement() {
        GamepadInputCache.setAxes(1f,0f,0f,10,2000)
        GamepadInputCache.setButton(16,true,20,2010)
        assertTrue(ControllerInputCache.consume()!!.frame.shoot)
        GamepadInputCache.clearDevice(10,2200)
        assertNull(ControllerInputCache.latest.get())
        GamepadInputCache.publish(2300)
        assertNull(ControllerInputCache.latest.get())
    }
    @Test fun rightStickDownIsProneWhileYHasNoPauseBinding() {
        GamepadInputCache.setAxes(1f,-1f,0f,20,2000,rightY=-1f)
        assertTrue(ControllerInputCache.consume()!!.frame.prone)
        GamepadInputCache.setButton(8,true,20,2010)
        assertFalse(ControllerInputCache.consume()!!.frame.pause)
        GamepadInputCache.setAxes(1f,-1f,0f,20,2020,rightY=0f)
        assertFalse(ControllerInputCache.consume()!!.frame.prone)
        GamepadInputCache.clearDevice(20,2030)
        assertNull(ControllerInputCache.latest.get())
    }
}
