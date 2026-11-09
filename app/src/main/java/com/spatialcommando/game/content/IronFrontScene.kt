package com.spatialcommando.game.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import android.util.Log
import androidx.compose.ui.unit.dp
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.design.LinearProgressIndicator
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.platform.LengthUnit
import com.pico.spatial.ui.platform.LocalPhysicalLengthConverter
import com.spatialcommando.game.game.GameMode
import com.spatialcommando.game.game.GameRuntime

@Composable
fun IronFrontScene() {
    val context = LocalContext.current
    val converter=LocalPhysicalLengthConverter.current
    val width=converter.dpToLength(1200.dp,LengthUnit.Meters)
    val height=converter.dpToLength(720.dp,LengthUnit.Meters)
    val depth=converter.dpToLength(480.dp,LengthUnit.Meters)
    val hud=GameRuntime.hud
    val ownerRef=remember {arrayOfNulls<Entity>(1)}
    val environmentRef = remember { arrayOfNulls<JungleEnvironment>(1) }
    DisposableEffect(Unit) {onDispose {
        if(GameRuntime.sceneRoot===ownerRef[0])GameRuntime.dispose()
        environmentRef[0]?.close()
    }}
    SpatialView(
        modifier=Modifier.fillMaxSize().testTag("iron-front-game"),
        attachments={
            AttachmentPanel("hud") {GameHud()}
            AttachmentPanel("enemyHud") {EnemyHud()}
            AttachmentPanel("title") {Box(Modifier.size(340.dp,240.dp),contentAlignment=Alignment.Center){if(hud.mode==GameMode.TITLE)TitlePanel()}}
            AttachmentPanel("pause") {Box(Modifier.size(400.dp,300.dp),contentAlignment=Alignment.Center){if(hud.mode==GameMode.PAUSED)PausePanel()}}
            AttachmentPanel("result") {Box(if(hud.mode==GameMode.LOST)Modifier.size(240.dp,104.dp) else Modifier.size(400.dp,300.dp),contentAlignment=Alignment.Center){if(hud.mode==GameMode.WON||hud.mode==GameMode.LOST)ResultPanel()}}
            AttachmentPanel("footer") {Box(Modifier.size(232.dp,256.dp),contentAlignment=Alignment.TopCenter){ControlsHud()}}
            AttachmentPanel("progress") {Box(Modifier.size(1136.dp,8.dp),contentAlignment=Alignment.Center){LinearProgressIndicator(progress={hud.progress},modifier=Modifier.fillMaxWidth().height(4.dp))}}
        },
        initial={content,attachments->
            GameRuntime.initializeCampaign(context)
            if(GameRuntime.audio==null)GameRuntime.audio=com.spatialcommando.game.game.GameAudio(context)
            val owner=Entity();owner.setName("IronFrontDisplay");ownerRef[0]=owner;GameRuntime.sceneRoot=owner
            // Scale scenery, actors and SpatialUI attachments together in the Stage.
            val displayScale = 3.2f / width
            owner.components[TransformComponent::class.java]!!.apply {
                position = Vector3(0f, 1.45f, -2.2f)
                scaleVector = Vector3(displayScale, displayScale, displayScale)
            }
            content.addEntity(owner)
            bindBattlefieldAccessibility(content,owner);bindPanelAccessibility(content,attachments,owner,width,height,depth)
            try {
                val environment = JungleEnvironment.load(context)
                environmentRef[0] = environment
                bindEnvironmentAccessibility(content, environment.root)
                val scene = loadEditorGameScene(context)
                val models = loadGeneratedModels(context)
                val renderer = GameModelRenderer(owner, scene.loaded, scene.scope, width, height, depth, models, environment::applyTheme)
                GameRuntime.renderer = renderer
                GameRuntime.sceneReady = true
                GameRuntime.loadingText = "丛林前线已就绪"
                GameRuntime.publish()
                renderer.update(GameRuntime.engine)
                Log.i("IronFront", "Headset display width=3.2m center=(0,1.45,-2.2) scale=$displayScale")
            } catch (e: Exception) {
                Log.e("IronFront", "Editor scene integration failed", e)
                GameRuntime.sceneReady = false
                GameRuntime.loadingText = "场景加载失败，请重新打开游戏"
            }
        },
        update={_,attachments->
            GameRuntime.panelEntities["title"]?.enabled=hud.mode==GameMode.TITLE
            GameRuntime.panelEntities["pause"]?.enabled=hud.mode==GameMode.PAUSED
            GameRuntime.panelEntities["result"]?.enabled=hud.mode==GameMode.WON||hud.mode==GameMode.LOST
        },
    )
}
