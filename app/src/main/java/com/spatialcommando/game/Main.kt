package com.spatialcommando.game

import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultStage
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import com.spatialcommando.game.content.GameWindow

fun mainApp(scope: SpatialAppScope) = with(scope) {
    DefaultStage { PicoTheme { GameWindow() } }
}
