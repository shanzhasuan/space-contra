package com.spatialcommando.game.game

import android.os.SystemClock
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.SceneUpdateContext
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

interface SceneRenderTarget {
    val root: Entity
    fun update(engine: GameEngine)
    fun close()
}
data class ControllerSnapshot(val frame: InputFrame, val receivedAt: Long)
object ControllerInputCache {
    val latest = AtomicReference<ControllerSnapshot?>(null)
    val lastNativeReceivedAt = AtomicLong(0)
    fun submitNative(frame: InputFrame, receivedAt: Long) { lastNativeReceivedAt.set(receivedAt); submit(frame, receivedAt) }
    private val pressed = AtomicInteger(0)
    fun submit(frame: InputFrame, receivedAt: Long) {
        val old = latest.getAndSet(ControllerSnapshot(frame,receivedAt))?.frame ?: InputFrame()
        var bits=0
        if(frame.jump&&!old.jump)bits=bits or 1
        if(frame.switch&&!old.switch)bits=bits or 2
        if(frame.pause&&!old.pause)bits=bits or 4
        if(frame.back&&!old.back)bits=bits or 8
        if(frame.shoot&&!old.shoot)bits=bits or 16
        if(bits!=0)pressed.getAndUpdate {it or bits}
    }
    fun consume(): ControllerSnapshot? {
        val state=latest.get() ?: return null
        val bits=pressed.getAndSet(0)
        return state.copy(frame=state.frame.copy(jump=state.frame.jump||bits and 1!=0,
            switch=state.frame.switch||bits and 2!=0,pause=state.frame.pause||bits and 4!=0,
            back=state.frame.back||bits and 8!=0,shoot=state.frame.shoot||bits and 16!=0))
    }
    fun clear() { latest.set(null);pressed.set(0);lastNativeReceivedAt.set(0) }
}

/** UI observes coarse HUD changes; all continuous game/entity work remains in ECS. */
object GameRuntime {
    val engine = GameEngine()
    var hud by mutableStateOf(engine.hud()); private set
    var sceneReady by mutableStateOf(false)
    var sceneLoadFailure: String? = null
    var loadingText by mutableStateOf("正在加载闯关场景…")
    var controlsReady by mutableStateOf(false)
    var controlText by mutableStateOf("请连接 PICO 左右手柄")
    var fullSpaceReady = false
    var renderer: SceneRenderTarget? = null
    var audio: GameAudio? = null
    var sceneRoot: Entity? = null
    val panelEntities = mutableMapOf<String, Entity>()
    private var lastHudAt = 0L
    private var preferences: SharedPreferences?=null
    private var savedLevel=-1
    private var savedPoints=-1
    private var savedCleared=-1
    fun initializeCampaign(context: Context) {
        if(preferences!=null)return
        val prefs=context.getSharedPreferences("space_contra_campaign_v1",Context.MODE_PRIVATE)
        preferences=prefs;savedCleared=prefs.getInt("cleared_levels",0).coerceIn(0,10)
        savedLevel=prefs.getInt("checkpoint_level",0).coerceIn(0,savedCleared.coerceAtMost(9));savedPoints=if(savedCleared==0)0 else prefs.getInt("checkpoint_score",0).coerceAtLeast(0)
        engine.restoreCheckpoint(savedLevel,savedPoints);publish()
    }
    fun publish() {
        hud = engine.hud()

        engine.completedMissionCheckpoint()?.let { saveCheckpoint(it) }
    }
    private fun saveCheckpoint(checkpoint: CampaignCheckpoint) {
        if(preferences!=null && (savedLevel!=checkpoint.levelIndex || savedPoints!=checkpoint.score || savedCleared!=checkpoint.clearedLevels)) {
            savedLevel=checkpoint.levelIndex;savedPoints=checkpoint.score;savedCleared=checkpoint.clearedLevels
            preferences!!.edit().putInt("checkpoint_level",savedLevel).putInt("checkpoint_score",savedPoints)
                .putInt("cleared_levels",savedCleared).apply()
        }
    }
    fun start() { if (sceneReady) { audio?.stopAll();engine.start(); publish() } }
    fun newCampaign() { if(sceneReady) { audio?.stopAll();engine.startFresh(); saveCheckpoint(CampaignCheckpoint(0,0,0)); publish() } }
    fun restart() { if(sceneReady) { audio?.stopAll();engine.restartLevel(); publish() } }
    fun nextLevel() { if(sceneReady) { audio?.stopAll();engine.advanceLevel(); publish() } }
    fun pause() { engine.pause();audio?.stopAll();engine.drainSoundEvents(); publish() }
    fun resume() { engine.resume(); publish() }
    fun title() { audio?.stopAll();engine.drainSoundEvents();engine.returnToTitle(); publish() }
    fun dispose() {
        audio?.close();audio=null;engine.drainSoundEvents()
        engine.pause(); if(renderer!=null)renderer?.close() else sceneRoot?.destroy(); sceneRoot=null; panelEntities.clear(); renderer = null; sceneReady = false; controlsReady = false
        ControllerInputCache.clear(); publish()
    }
    fun tick(context: SceneUpdateContext) {
        val view = renderer ?: return
        if (view.root.scene != context.scene) return
        val now = SystemClock.elapsedRealtime()
        GamepadInputCache.publish(now)
        val input = ControllerInputCache.latest.get()
        val ready = fullSpaceReady && input != null && now - input.receivedAt < 1000L
        if (!ready && controlsReady) engine.pause()
        controlsReady = ready
        if (ready) engine.update(context.deltaTime, ControllerInputCache.consume()!!.frame)
        view.update(engine)
        val soundEvents=engine.drainSoundEvents()
        if(engine.mode==GameMode.PAUSED)audio?.stopAll() else audio?.play(soundEvents)
        if (now - lastHudAt > 80 || engine.mode != hud.mode) { publish(); lastHudAt = now }
    }
}

class CommandoGameSystem : com.pico.spatial.core.ecs.System() {
    override fun update(context: SceneUpdateContext) { GameRuntime.tick(context) }
}
