package com.spatialcommando.game

import com.spatialcommando.game.game.ControllerInputCache
import com.spatialcommando.game.game.InputFrame
import org.junit.Assert.*
import org.junit.Test

class ControllerInputCacheTest {
    @Test fun releasedTapSurvivesUntilSceneFrameConsumesIt() {
        ControllerInputCache.clear()
        ControllerInputCache.submit(InputFrame(jump=true),10L)
        ControllerInputCache.submit(InputFrame(),11L)
        assertTrue(ControllerInputCache.consume()!!.frame.jump)
        assertFalse(ControllerInputCache.consume()!!.frame.jump)
        ControllerInputCache.clear()
    }
}
