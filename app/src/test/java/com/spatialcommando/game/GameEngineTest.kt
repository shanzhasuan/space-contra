package com.spatialcommando.game

import com.spatialcommando.game.game.*
import org.junit.Assert.*
import org.junit.Test

class GameEngineTest {
    private fun frames(g: GameEngine, count: Int, input: InputFrame = InputFrame()) { repeat(count) { g.update(1f / 60f, input) } }
    @Test fun jumpReturnsToSolidGround() {
        val g = GameEngine(); g.start(); g.update(1f / 60f, InputFrame(jump=true)); assertTrue(g.playerY > 0f)
        frames(g, 90); assertTrue(g.grounded); assertEquals(0f, g.playerY, .001f)
    }
    @Test fun pauseFreezesSimulationAndPreservesProgress() {
        val g=GameEngine();g.start();frames(g,30,InputFrame(x=1f)); val before=g.playerX;val elapsed=g.elapsed
        g.pause();frames(g,100,InputFrame(x=1f,shoot=true));assertEquals(before,g.playerX,0f);assertEquals(elapsed,g.elapsed,0f)
        g.resume();frames(g,10,InputFrame(x=1f));assertTrue(g.playerX>before)
    }
    @Test fun longFrameIsClampedAndInvalidTimeIsIgnored() {
        val g=GameEngine();g.start();g.update(12f,InputFrame(x=1f));assertTrue(g.playerX<.8f)
        val before=g.playerX;g.update(Float.NaN,InputFrame(x=1f));assertEquals(before,g.playerX,0f)
    }
    @Test fun heldJumpDoesNotRepeatOnLanding() {
        val g=GameEngine();g.start();frames(g,150,InputFrame(jump=true));assertTrue(g.grounded);assertEquals(0f,g.playerY,.001f)
    }
    @Test fun weaponPickupCreatesSpreadFanAndDeathResetsWeapon() {
        val g=GameEngine();g.start();g.enemies.clear();frames(g,67,InputFrame(x=1f));assertTrue(g.spreadUnlocked);assertEquals(Weapon.SPREAD,g.weapon)
        g.update(1f/30f,InputFrame(shoot=true));assertEquals(5,g.bullets.count{it.friendly});assertTrue(g.bullets.any{it.vy<0});assertTrue(g.bullets.any{it.vy>0})
        g.damage(true);assertEquals(Weapon.RIFLE,g.weapon);assertFalse(g.spreadUnlocked)
    }
    @Test fun immunityPreventsRepeatedLifeLoss() {
        val g=GameEngine();g.start();frames(g,110);g.damage();assertEquals(4,g.lives);g.damage();assertEquals(4,g.lives)
    }
    @Test fun fifthFatalFallEndsGameAndRestartResetsMission() {
        val g=GameEngine();g.start();repeat(5){g.damage(true)};assertEquals(GameMode.LOST,g.mode)
        g.start();assertEquals(5,g.lives);assertEquals(GameMode.PLAYING,g.mode);assertTrue(g.enemies.any{it.type==2&&it.health==36})
    }
    @Test fun fastSegmentDetectsCrossingSmallTarget() {
        assertTrue(GameEngine.segmentHitsRect(-10f,.5f,10f,.5f,-.1f,0f,.1f,1f))
        assertFalse(GameEngine.segmentHitsRect(-10f,1.5f,10f,1.5f,-.1f,0f,.1f,1f))
    }
    @Test fun defeatingGuardianCompletesMission() {
        val g=GameEngine();g.start();g.enemies.clear();g.enemies+=Enemy(800,2,1.3f,health=1)
        g.update(.1f,InputFrame(shoot=true));assertEquals(GameMode.WON,g.mode);assertEquals(5000,g.score)
    }
}
