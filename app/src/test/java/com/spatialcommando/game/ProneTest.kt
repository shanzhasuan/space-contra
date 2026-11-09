package com.spatialcommando.game

import com.spatialcommando.game.game.*
import org.junit.Assert.*
import org.junit.Test

class ProneTest {
    private fun game(): GameEngine = GameEngine().also { it.start();it.enemies.clear() }
    private fun vulnerable(): GameEngine = game().also {g->repeat(20){g.update(0.1f,InputFrame())}}
    @Test fun fiveLivesAreAvailableOnStartAndRetry() {
        val g=game();assertEquals(5,g.lives);assertEquals(5,g.hud().maxLives)
        repeat(4){g.damage(true)};assertEquals(1,g.lives);assertEquals(GameMode.PLAYING,g.mode)
        g.damage(true);assertEquals(GameMode.LOST,g.mode)
        g.restartLevel();assertEquals(5,g.lives);assertEquals(GameMode.PLAYING,g.mode)
    }
    @Test fun rightProneInputHasPriorityOverLeftCrouchAndDoesNotMove() {
        val g=game();val start=g.playerX
        g.update(0.1f,InputFrame(x=1f,y=-1f,prone=true))
        assertTrue(g.prone);assertFalse(g.crouching);assertEquals(start,g.playerX,0f)
        g.update(0.1f,InputFrame(y=-1f));assertFalse(g.prone);assertTrue(g.crouching)
        g.update(0.1f,InputFrame());assertFalse(g.prone);assertFalse(g.crouching)
    }
    @Test fun proneCanShootHorizontallyAndJumpCanLeaveProne() {
        val g=game();g.update(0.1f,InputFrame(y=1f,prone=true,shoot=true))
        val shot=g.bullets.single {it.friendly}
        assertEquals(0f,shot.vy,0.0001f);assertEquals(0.12f,shot.y,0.001f)
        g.update(1f/60f,InputFrame(prone=true,jump=true));assertFalse(g.prone);assertFalse(g.grounded);assertTrue(g.playerY>0f)
        g.update(1f/60f,InputFrame(prone=true));assertFalse(g.prone)
    }
    @Test fun highBulletHitsStandingButPassesOverProneAndLowBulletStillHits() {
        val standing=vulnerable();standing.bullets+=Bullet(900,standing.playerX-0.6f,0.45f,9f,0f,false)
        standing.update(0.1f,InputFrame());assertEquals(4,standing.lives)
        val prone=vulnerable();prone.bullets+=Bullet(900,prone.playerX-0.6f,0.45f,9f,0f,false)
        prone.update(0.1f,InputFrame(prone=true));assertEquals(5,prone.lives)
        val low=vulnerable();low.bullets+=Bullet(900,low.playerX-0.6f,0.14f,9f,0f,false)
        low.update(0.1f,InputFrame(prone=true));assertEquals(4,low.lives)
    }
}