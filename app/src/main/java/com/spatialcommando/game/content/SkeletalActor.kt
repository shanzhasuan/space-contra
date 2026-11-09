package com.spatialcommando.game.content

import android.util.Log
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.animation.AnimationPlayConfig
import com.pico.spatial.core.ecs.animation.AnimationPlaybackController
import com.pico.spatial.core.ecs.resource.AnimationResource

/** Owns playback independently of model instancing; no animation controller is copied. */
class SkeletalActor private constructor(private val target: Entity, private val clips: Map<String,AnimationResource>) {
    private val loops=linkedMapOf<String,AnimationResource>()
    private var controller: AnimationPlaybackController?=null
    private var current=""
    val supportsProne get()=clips.containsKey("prone")
    val available get()=clips.keys.containsAll(listOf("idle","walk","jump","crouch"))
    fun update(desired: String, paused: Boolean, speed: Float=1f) {
        val old=controller
        if(paused) { if(old?.valid==true && old.isPlaying()) old.pause(); return }
        if(old?.valid==true && old.isPaused()) old.resume()
        val loop=desired!="jump"
        if(desired==current && !(loop && old?.valid==true && old.isComplete())) return
        if(old?.valid==true)old.stop()
        old?.close()
        val resource=if(loop)loops.getOrPut(desired) { requireNotNull(clips[desired]).repeat(16).also { it.toGlobal() } } else requireNotNull(clips[desired])
        controller=target.playAnimation(resource,AnimationPlayConfig(transitionDuration=0.06f)).also { it.setSpeed(speed) }
        current=desired
    }
    fun close() {
        controller?.let { if(it.valid)it.stop();it.close() };controller=null
        loops.values.forEach { it.close() };loops.clear()
    }
    companion object {
        fun create(actor: Entity, required: Boolean=false): SkeletalActor? {
            val targets=actor.findSkeletonRootEntity(includeDisabled=true).toList()+actor.findSkinnedMeshEntity(includeDisabled=true).toList()
            for(target in targets.distinct()) {
                val clips=target.getAnimationResources().associateBy { it.getName() }
                if(clips.keys.containsAll(listOf("idle","walk","jump","crouch"))) {
                    Log.i("IronFront","Skeletal actor=${actor.getName()} clips=${clips.keys}")
                    return SkeletalActor(target,clips)
                }
            }
            if(required)error("Missing actor skeleton or clips: ${actor.getName()}")
            return null
        }
    }
}
