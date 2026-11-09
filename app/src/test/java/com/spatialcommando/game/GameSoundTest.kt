package com.spatialcommando.game

import com.spatialcommando.game.game.*
import org.junit.Assert.*
import org.junit.Test

class GameSoundTest {
    private fun game()=GameEngine().also {it.start();it.enemies.clear();it.drainSoundEvents()}
    @Test fun heldTriggerEmitsSoundOnlyWhenAShotActuallyFires() {
        val g=game();repeat(6){g.update(1f/120f,InputFrame(shoot=true))}
        assertEquals(1,g.drainSoundEvents().count {it==GameSound.PLAYER_SHOT})
        g.pause();repeat(20){g.update(1f/60f,InputFrame(shoot=true))}
        assertTrue(g.drainSoundEvents().isEmpty())
    }
    @Test fun realDamageAndImmunityDoNotRepeatHurtSound() {
        val g=game();g.damage(true);g.damage()
        assertEquals(listOf(GameSound.HURT),g.drainSoundEvents())
        repeat(4){g.damage(true)}
        assertEquals(1,g.drainSoundEvents().count {it==GameSound.GAME_OVER})
    }
    @Test fun guardianDefeatEmitsExplosionAndClearOnce() {
        val g=game();g.enemies+=Enemy(900,2,1.3f,health=1)
        g.update(0.1f,InputFrame(shoot=true));val effects=g.drainSoundEvents()
        assertTrue(effects.containsAll(listOf(GameSound.PLAYER_SHOT,GameSound.EXPLOSION,GameSound.CLEAR)))
        g.update(0.1f,InputFrame(shoot=true));assertTrue(g.drainSoundEvents().isEmpty())
    }
    @Test fun pickupSoundIsEmittedOncePerCollectedItem() {
        val g=game();repeat(67){g.update(1f/60f,InputFrame(x=1f))}
        assertEquals(1,g.drainSoundEvents().count {it==GameSound.PICKUP})
    }
}