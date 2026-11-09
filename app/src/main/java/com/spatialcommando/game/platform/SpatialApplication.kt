package com.spatialcommando.game.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import com.spatialcommando.game.mainApp

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        launch(::mainApp)
    }
}
