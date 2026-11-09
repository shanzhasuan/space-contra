package com.spatialcommando.game

import com.spatialcommando.game.game.*
import org.junit.Assert.*
import org.junit.Test

class EnemyHudTest {
    @Test fun projectileHitUpdatesRemainingHealthWithoutChangingMaximum() {
        val game=GameEngine();game.start();game.enemies.clear()
        game.enemies+=Enemy(100,0,1.3f,health=3,cooldown=100f,walk=0f)
        assertEquals(3,game.hud().targetHealth)
        game.update(0.1f,InputFrame(shoot=true))
        assertEquals(2,game.hud().targetHealth);assertEquals(3,game.hud().targetMaxHealth)
        assertEquals("拦路敌兵",game.hud().targetName)
    }
    @Test fun mostRecentlyDamagedOpponentRemainsSelectedOverCloserOpponent() {
        val game=GameEngine();game.start();game.enemies.clear()
        game.enemies+=Enemy(100,0,1.3f,health=4,cooldown=100f,walk=0f)
        game.update(0.1f,InputFrame(shoot=true))
        game.enemies+=Enemy(101,1,0.8f,health=10,cooldown=100f)
        assertEquals("拦路敌兵",game.hud().targetName)
        assertEquals(3,game.hud().targetHealth);assertEquals(4,game.hud().targetMaxHealth)
    }
    @Test fun defeatedTargetShowsZeroThenNextBlockerAndRestartRestoresHealth() {
        val game=GameEngine();game.start();game.enemies.clear()
        game.enemies+=Enemy(100,0,1.3f,health=1,cooldown=100f,walk=0f)
        game.enemies+=Enemy(101,1,2.8f,health=5,cooldown=100f)
        game.update(0.1f,InputFrame(shoot=true))
        assertEquals(0,game.hud().targetHealth);assertEquals(1,game.hud().targetMaxHealth)
        game.pause();assertEquals(0,game.hud().targetHealth);game.resume()
        repeat(10){game.update(0.1f,InputFrame())}
        assertEquals("拦路炮台",game.hud().targetName);assertEquals(5,game.hud().targetHealth)
        game.restartLevel()
        assertEquals("拦路敌兵",game.hud().targetName);assertEquals(2,game.hud().targetHealth)
        assertEquals(2,game.hud().targetMaxHealth)
        game.returnToTitle();assertEquals(0,game.hud().targetMaxHealth)
    }
}