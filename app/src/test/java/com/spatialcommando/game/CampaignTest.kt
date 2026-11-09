package com.spatialcommando.game

import com.spatialcommando.game.game.*
import org.junit.Assert.*
import org.junit.Test

class CampaignTest {
    private fun win(g: GameEngine) {
        g.enemies.clear()
        g.enemies += Enemy(800, 2, 1.3f, health = 1)
        g.update(0.1f, InputFrame(shoot = true))
        assertEquals(GameMode.WON, g.mode)
    }
    @Test fun tenActualWinsAdvanceAndCompleteCampaign() {
        val game = GameEngine(); game.start()
        repeat(10) { index ->
            assertEquals(index + 1, game.hud().levelNumber)
            assertEquals(36 + index * 8, game.enemies.single { it.type == 2 }.health)
            assertEquals(5 + index, game.enemies.count { it.type == 0 })
            win(game)
            if (index < 9) assertTrue(game.advanceLevel())
        }
        assertTrue(game.campaignComplete)
        assertFalse(game.advanceLevel())
        assertEquals(50000, game.score)
    }
    @Test fun cannotSkipAnUnfinishedLevelAndRestartKeepsCheckpoint() {
        val game = GameEngine(); game.start()
        assertFalse(game.advanceLevel())
        repeat(3) { win(game); assertTrue(game.advanceLevel()) }
        repeat(5) { game.damage(true) }
        assertEquals(GameMode.LOST, game.mode)
        game.restartLevel()
        assertEquals(4, game.hud().levelNumber)
        assertEquals(15000, game.score)
        assertEquals(5, game.lives)
        assertEquals(GameMode.PLAYING, game.mode)
    }
    @Test fun checkpointCanResumeLevelSixAndStartFresh() {
        val game=GameEngine();game.restoreCheckpoint(5,23000);game.start()
        assertEquals(6,game.hud().levelNumber);assertEquals(23000,game.score)
        repeat(3){game.damage(true)};game.restartLevel();assertEquals(6,game.hud().levelNumber)
        game.startFresh();assertEquals(1,game.hud().levelNumber);assertEquals(0,game.score)
    }
    @Test fun everyElevatedPlatformIsBelowObservedJumpApex() {
        val game=GameEngine(); game.start()
        game.update(1f/60f,InputFrame(jump=true))
        var apex=game.playerY
        repeat(90) { game.update(1f/60f,InputFrame());apex=maxOf(apex,game.playerY) }
        for(level in CampaignLevels.all) for(platform in level.platforms) assertTrue(platform.y<apex)
    }
    @Test fun layoutsHaveReachableGapsAndSupportedBossPositions() {
        assertEquals(10, CampaignLevels.all.map { it.theme }.distinct().size)
        for (level in CampaignLevels.all) {
            val ground = level.platforms.filter { it.ground }
            for ((left, right) in ground.zipWithNext()) {
                assertTrue(right.x - (left.x + left.width) < 1.5f)
            }
            assertTrue(ground.any { level.bossX >= it.x && level.bossX <= it.x + it.width })
            assertTrue(level.platforms.all { it.width > 1f })
        }
    }
    @Test fun fireAndWeaponButtonsDoNotNavigateOrResetFromMenus() {
        val game=GameEngine();game.restoreCheckpoint(5,23000)
        game.update(0.1f,InputFrame(switch=true,shoot=true,back=true))
        assertEquals(GameMode.TITLE,game.mode)
        assertEquals(6,game.hud().levelNumber);assertEquals(23000,game.score)
        game.start();game.update(0.1f,InputFrame(x=1f));game.pause()
        val pausedX=game.playerX
        game.update(0.1f,InputFrame(switch=true,shoot=true,back=true))
        assertEquals(GameMode.PAUSED,game.mode)
        assertEquals(pausedX,game.playerX,0f)
        assertEquals(6,game.hud().levelNumber);assertEquals(23000,game.score)
        game.update(0.1f,InputFrame());game.update(0.1f,InputFrame(jump=true))
        assertEquals(GameMode.PLAYING,game.mode)
    }
}
