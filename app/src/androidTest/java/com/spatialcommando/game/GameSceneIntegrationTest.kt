package com.spatialcommando.game

import android.content.Intent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.StageEnvironmentLightingComponent
import com.pico.spatial.core.ecs.ImageBasedLightSource
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.spatialcommando.game.game.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator-only integration test. Synthetic snapshots test ECS/rendering, not physical controllers. */
@RunWith(AndroidJUnit4::class)
class GameSceneIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val feeding=AtomicBoolean(false)
    private val feedFrame=AtomicReference(InputFrame())
    private fun onMain(action: () -> Unit) { instrumentation.runOnMainSync(action) }
    private fun drive(frames: Int, frame: InputFrame) {
        feedFrame.set(frame)
        repeat(frames) {
            ControllerInputCache.submit(frame,SystemClock.elapsedRealtime())
            Thread.sleep(16)
        }
    }
    private fun worldX(): Float {var result=0f;onMain{result=GameRuntime.engine.playerX};return result}
    private fun renderedPosition(): Pair<Float,Float> {
        var value=0f to 0f
        onMain {
            val player=GameRuntime.renderer?.root?.findEntity("PlayerActor")
            val position=player?.components?.get(TransformComponent::class.java)?.position
            value=(position?.x?:Float.NaN) to (position?.y?:Float.NaN)
        }
        return value
    }
    private fun waitForFrame(frame: InputFrame, timeout: Long = 5000L, condition: () -> Boolean): Boolean {
        feedFrame.set(frame)
        val until = SystemClock.elapsedRealtime() + timeout
        while (SystemClock.elapsedRealtime() < until) {
            ControllerInputCache.submit(frame,SystemClock.elapsedRealtime())
            if (condition()) return true
            Thread.sleep(30)
        }
        return false
    }
    private fun state(): String {
        var result=""
        onMain { result="mode=${GameRuntime.engine.mode} grounded=${GameRuntime.engine.grounded} x=${GameRuntime.engine.playerX} y=${GameRuntime.engine.playerY} velocityY=${GameRuntime.engine.velocityY} elapsed=${GameRuntime.engine.elapsed} controls=${GameRuntime.controlsReady}" }
        return result
    }
    @Test fun loadedEditorSceneMovesJumpsAndFreezesThroughEcs() {
        val context=instrumentation.targetContext
        assertTrue(context.getSharedPreferences("space_contra_campaign_v1",0).edit().clear().commit())
        val intent=context.packageManager.getLaunchIntentForPackage(context.packageName)!!
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        val deadline=SystemClock.elapsedRealtime()+90000
        var ready=false
        while(!ready&&SystemClock.elapsedRealtime()<deadline){
            var failure: String? = null
            onMain{ready=GameRuntime.sceneReady;failure=GameRuntime.sceneLoadFailure}
            if(failure!=null) fail("Editor scene loading failed: $failure")
            Thread.sleep(100)
        }
        assertTrue("Editor scene must load before native integration test",ready)
        feeding.set(true)
        val producer=Thread { while(feeding.get()) {ControllerInputCache.submit(feedFrame.get(),SystemClock.elapsedRealtime());Thread.sleep(16)} }
        producer.start()
        try {
            onMain {
                ControllerInputCache.clear();GameRuntime.start()
                // Keep animation observation deterministic while retaining real enemy models and patrols.
                GameRuntime.engine.enemies.forEach {it.cooldown=100f}
                repeat(18){GameRuntime.engine.update(0.1f,InputFrame())}
                GameRuntime.renderer!!.update(GameRuntime.engine);GameRuntime.publish()
            }
            onMain{assertEquals(5,GameRuntime.hud.lives);assertEquals(5,GameRuntime.hud.maxLives)}
            onMain {
                fun tree(entity: com.pico.spatial.core.ecs.Entity): List<com.pico.spatial.core.ecs.Entity> = listOf(entity) + entity.getChildren().flatMap { tree(it) }
                val renderer=GameRuntime.renderer!!
                val root=renderer.root
                assertTrue("Generated archetype descendants must remain hidden",tree(root.findEntity("GeneratedArchetypes")!!).none {it.enabled})
                assertTrue("Title models must disappear during gameplay",tree(root.findEntity("JungleReference")!!).none {it.enabled})
                assertTrue("Retained hero skin must be visible",root.findEntity("PlayerActor")!!.findSkinnedMeshEntity(includeDisabled=true).all {it.enabled})
                val enemy=GameRuntime.engine.enemies.first()
                val enemyModel=root.findEntity("Enemy_${enemy.id}")!!
                val health=enemy.health;val x=enemy.x
                enemy.health=0;renderer.update(GameRuntime.engine)
                assertTrue("Every mesh of a defeated actor must disappear",tree(enemyModel).none {it.enabled})
                enemy.health=health;enemy.x=GameRuntime.engine.cameraX+20f;renderer.update(GameRuntime.engine)
                assertTrue("Offscreen actor descendants must remain hidden",tree(enemyModel).none {it.enabled})
                enemy.x=x;renderer.update(GameRuntime.engine)
                assertTrue("Returning live actors must restore their meshes",tree(enemyModel).all {it.enabled})
                val left=GameRuntime.panelEntities["hud"]!!.components[TransformComponent::class.java]!!
                val right=GameRuntime.panelEntities["enemyHud"]!!.components[TransformComponent::class.java]!!
                assertEquals(-left.position.x,right.position.x,0.0001f)
                assertEquals(left.position.y,right.position.y,0.0001f)
                assertEquals(left.position.z,right.position.z,0.0001f)
                assertEquals(0f,left.eulerAngles.yaw,0.0001f);assertEquals(0f,right.eulerAngles.yaw,0.0001f)
                assertTrue("HUD plane must be in front of actor geometry",left.position.z>0f)
                println("SPACE_CONTRA_VISIBILITY_HUD_TEST_PASS prototypes=hidden title=hidden dead=hidden offscreen=hidden restored=true hud=mirrored")
            }

            assertTrue("All eleven sound effects must decode",waitForFrame(InputFrame(),5000){var ready=0;onMain{ready=GameRuntime.audio!!.readyCount};ready==11})
            assertTrue("Game start sound must play through native SoundPool",waitForFrame(InputFrame(),5000){var played=0;onMain{played=GameRuntime.audio!!.successfulPlays};played>0})
            println("SPACE_CONTRA_AUDIO_TEST_PASS loaded=11")
            drive(5,InputFrame())
            val before=renderedPosition()
            assertTrue("Player is a real Editor model",before.first.isFinite()&&before.second.isFinite())
            val moved=waitForFrame(InputFrame(x=1f),10000){worldX()>1f && renderedPosition().first>before.first+0.01f}
            assertTrue("Native scene must advance movement: ${state()}",moved)
            val after=renderedPosition()
            assertTrue("ECS must advance logical motion",worldX()>1f)
            assertTrue("ECS must move the rendered model",after.first>before.first+0.01f)
            // SDK6.1 exposes native animation playback, but not rendered SkeletonPoseComponent.
            // Joint Entity transforms are import metadata and are not a skin-pose readback.
            fun playback(enemy: Int? = null): com.spatialcommando.game.content.ActorPlaybackState? {
                var value: com.spatialcommando.game.content.ActorPlaybackState? = null
                onMain { value=(GameRuntime.renderer!! as com.spatialcommando.game.content.GameModelRenderer).actorPlaybackState(enemy) }
                return value
            }
            onMain {
                for(name in listOf("PlayerActor","Enemy_1")) {
                    val actor=GameRuntime.renderer!!.root.findEntity(name)!!
                    val meshes=actor.findSkinnedMeshEntity(includeDisabled=true)
                    assertTrue("Imported actor must contain a skinned mesh: $name",meshes.isNotEmpty())
                    assertTrue("Actor skeleton joints must be retained: $name",meshes.any { it.components[ModelComponent::class.java]!!.mesh.getSkeleton()?.joints?.size?.let { count -> count >= 8 } == true })
                }
            }
            assertTrue("Native walk clip must play",waitForFrame(InputFrame(x=1f),5000){playback()?.let {it.clip=="walk"&&it.playing}==true})
            val walkTime=playback()!!.time
            assertTrue("Native walk playback clock must advance",waitForFrame(InputFrame(x=1f),5000){playback()?.let {it.clip=="walk"&&it.time>walkTime+0.02f}==true})
            println("IRON_FRONT_ANIMATION_PLAYBACK_TEST_PASS clip=walk time=${playback()!!.time}")
            val enemyTime=playback(1)!!.time
            assertTrue("Cloned soldier must have independent native playback",waitForFrame(InputFrame(),5000){playback(1)?.let {it.clip=="walk"&&it.playing&&it.time>enemyTime+0.02f}==true})
            println("IRON_FRONT_ENEMY_ANIMATION_PLAYBACK_TEST_PASS time=${playback(1)!!.time}")
            drive(3,InputFrame());val proneX=worldX()
            val lying=waitForFrame(InputFrame(x=1f,prone=true),5000) {var isProne=false;onMain{isProne=GameRuntime.engine.prone};isProne&&playback()?.let {it.clip=="prone"&&it.playing}==true}
            assertTrue("Prone must play the real imported prone clip ${state()}",lying)
            assertEquals("Prone must freeze horizontal travel",proneX,worldX(),0.001f)
            drive(5,InputFrame(prone=true,shoot=true));onMain{assertTrue(GameRuntime.hud.prone)}
            val stood=waitForFrame(InputFrame(),5000){var upright=false;onMain{upright=!GameRuntime.engine.prone};upright&&playback()?.let {it.clip=="idle"&&it.playing}==true}
            assertTrue("Releasing right stick must restore native idle clip",stood)
            println("SPACE_CONTRA_PRONE_TEST_PASS clip=prone restored=${playback()!!.clip}")
            println("NATIVE_BEFORE_JUMP worldX=${worldX()} render=${renderedPosition()}")
            drive(1,InputFrame(jump=true))
            val jumped = waitForFrame(InputFrame()) {renderedPosition().second>after.second+0.005f}
            assertTrue("Jump must update rendered Y: before=$after after=${renderedPosition()} ${state()}",jumped)
            onMain{GameRuntime.pause()}
            val pausedX=worldX();val pausedRender=renderedPosition()
            drive(25,InputFrame(x=1f,shoot=true))
            assertEquals("Paused physics must freeze",pausedX,worldX(),0.0001f)
            assertEquals("Paused rendering must freeze",pausedRender.first,renderedPosition().first,0.0001f)
            onMain{GameRuntime.resume()}
            val resumed = waitForFrame(InputFrame(x=1f)) {worldX()>pausedX+0.01f}
            assertTrue("Resume must retain and advance mission ${state()}",resumed)
            println("IRON_FRONT_NATIVE_TEST_PASS logicalX=${worldX()} rendered=${renderedPosition()}")
            // Real rules reach ten wins in CampaignTest; here exercise resource replacement on every mission.
            var retainedHero: com.pico.spatial.core.ecs.Entity?=null
            onMain {retainedHero=GameRuntime.renderer!!.root.findEntity("PlayerActor")}
            val skyTextures=mutableSetOf<Any>();val lights=mutableSetOf<Any>()
            for(index in 0 until 10) {
                onMain {
                    GameRuntime.title();GameRuntime.engine.restoreCheckpoint(index,0);GameRuntime.start();GameRuntime.pause()
                    GameRuntime.renderer!!.update(GameRuntime.engine)
                    val root=GameRuntime.renderer!!.root
                    assertSame("Mission rebuild must retain the loaded hero rig",retainedHero,root.findEntity("PlayerActor"))
                    val sceneEntities=root.scene!!.queryEntity()
                    val sky=sceneEntities.single {it.getName()=="JunglePanoramaDome"}
                    val material=sky.components[ModelComponent::class.java]!!.materials[0] as UnlitMaterial
                    skyTextures.add(material.getBaseColorTexture())
                    val environment=sceneEntities.single {it.getName()=="JunglePanoramaEnvironment"}
                    lights.add((environment.components[StageEnvironmentLightingComponent::class.java]!!.source as ImageBasedLightSource.Single).resource)
                    val outdoor=GameRuntime.engine.level.theme in setOf("jungle","bridge","waterfall","snow")
                    assertEquals("Theme must switch mission geometry",outdoor,root.findEntity("MissionForest_0")!=null)
                    assertEquals("Indoor themes must instantiate SGS industrial scenery",!outdoor,root.findEntity("MissionIndustrial_0")!=null)
                    println("IRON_FRONT_MISSION_TEST_PASS level=${index+1} theme=${GameRuntime.engine.level.theme}")
                }
            }
            assertEquals("All ten missions must use different visible panorama textures",10,skyTextures.size)
            assertEquals("All ten missions must use different environment lighting resources",10,lights.size)
            onMain {
                GameRuntime.newCampaign()
                assertEquals(1,GameRuntime.engine.hud().levelNumber)
                GameRuntime.engine.enemies.clear();GameRuntime.engine.enemies+=Enemy(900,2,1.3f,health=1)
                GameRuntime.engine.update(0.1f,InputFrame(shoot=true));GameRuntime.publish()
                val progress=context.getSharedPreferences("space_contra_campaign_v1",0)
                assertEquals("Real mission win must persist next level",1,progress.getInt("checkpoint_level",-1))
                assertEquals(5000,progress.getInt("checkpoint_score",-1))
                assertEquals(1,progress.getInt("cleared_levels",-1))
                GameRuntime.title();GameRuntime.start()
                assertEquals(2,GameRuntime.engine.hud().levelNumber)
                println("SPACE_CONTRA_PROGRESS_TEST_PASS savedLevel=2 score=5000")
                GameRuntime.engine.enemies.clear();GameRuntime.engine.enemies+=Enemy(901,0,1.3f,health=3,cooldown=100f,walk=0f)
                GameRuntime.engine.update(0.1f,InputFrame(shoot=true));GameRuntime.publish()
                assertEquals(2,GameRuntime.hud.targetHealth);assertEquals(3,GameRuntime.hud.targetMaxHealth)
                assertTrue("Native enemy panel must remain attached",GameRuntime.panelEntities["enemyHud"]!!.valid)
                println("SPACE_CONTRA_ENEMY_HUD_TEST_PASS remaining=2 max=3 nativeComposeState=true")
            }
        } finally {feeding.set(false);producer.join(1000);onMain{ControllerInputCache.clear();GameRuntime.title()}}
    }
}
