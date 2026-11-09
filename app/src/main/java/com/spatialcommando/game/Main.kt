package com.spatialcommando.game

import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.Stage
import com.spatialcommando.game.content.CONTROLLER_STAGE
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import com.spatialcommando.game.content.GameWindow

fun mainApp(scope: SpatialAppScope) = with(scope) {
    Stage(id = CONTROLLER_STAGE) { PicoTheme { GameWindow() } }
}
