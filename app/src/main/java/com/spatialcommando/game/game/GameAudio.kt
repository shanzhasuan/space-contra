package com.spatialcommando.game.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.spatialcommando.game.R
import java.util.concurrent.ConcurrentHashMap

/** Low-latency, original NES-style short effects. Owns all audio resources. */
class GameAudio(context: Context) {
    private val pool=SoundPool.Builder().setMaxStreams(8).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val loaded=ConcurrentHashMap.newKeySet<Int>()
    private val ids=mutableMapOf<GameSound,Int>()
    private val streams=ArrayDeque<Int>()
    @Volatile private var closed=false
    var successfulPlays=0;private set
    val readyCount get()=loaded.size
    init {
        pool.setOnLoadCompleteListener {_,id,status->
            if(!closed&&status==0) {loaded.add(id);Log.i("SpaceContraAudio","Effect ready id=$id count=${loaded.size}")}
            else if(status!=0)Log.e("SpaceContraAudio","Effect loading failed id=$id status=$status")
        }
        val resources=mapOf(GameSound.PLAYER_SHOT to R.raw.player_fire,GameSound.SPREAD_SHOT to R.raw.spread_fire,
            GameSound.ENEMY_SHOT to R.raw.enemy_fire,GameSound.HIT to R.raw.impact,GameSound.EXPLOSION to R.raw.explosion,
            GameSound.JUMP to R.raw.jump,GameSound.HURT to R.raw.damage,GameSound.PICKUP to R.raw.pickup,
            GameSound.CLEAR to R.raw.level_clear,GameSound.GAME_OVER to R.raw.game_over,GameSound.START to R.raw.start_game)
        resources.forEach {(sound,resource)->ids[sound]=pool.load(context.applicationContext,resource,1)}
    }
    fun play(events: List<GameSound>) {
        if(closed)return
        for(sound in events.distinct()) {
            val id=ids[sound]?:continue
            if(id !in loaded)continue
            val volume=when(sound){GameSound.ENEMY_SHOT->0.10f;GameSound.EXPLOSION->0.40f;GameSound.HIT->0.22f;GameSound.START->0.24f;else->0.30f}
            val stream=pool.play(id,volume,volume,if(sound==GameSound.ENEMY_SHOT)1 else 2,0,1f)
            if(stream>0) {
                if(streams.size>=32)streams.removeFirst()
                streams.addLast(stream);successfulPlays++
                if(successfulPlays<=5||sound in setOf(GameSound.CLEAR,GameSound.GAME_OVER))Log.i("SpaceContraAudio","Playing $sound stream=$stream total=$successfulPlays")
            }
        }
    }
    fun stopAll() {if(closed)return;streams.forEach {pool.stop(it)};streams.clear()}
    fun close() {if(closed)return;stopAll();closed=true;pool.release();loaded.clear();ids.clear()}
}