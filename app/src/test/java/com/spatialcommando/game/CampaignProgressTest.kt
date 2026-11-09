package com.spatialcommando.game

import com.spatialcommando.game.game.*
import org.junit.Assert.*
import org.junit.Test

class CampaignProgressTest {
    private fun win(game: GameEngine) {
        game.enemies.clear();game.enemies+=Enemy(900,2,1.3f,health=1)
        game.update(0.1f,InputFrame(shoot=true))
        assertEquals(GameMode.WON,game.mode)
    }
    @Test fun firstCampaignStartsAtOneAndUnfinishedMissionsDoNotSave() {
        val game=GameEngine()
        assertEquals(1,game.hud().levelNumber);assertEquals(0,game.score)
        assertNull(game.completedMissionCheckpoint())
        game.start();game.update(0.1f,InputFrame(x=1f));game.pause()
        assertNull(game.completedMissionCheckpoint())
        game.resume();repeat(5){game.damage(true)}
        assertNull(game.completedMissionCheckpoint())
    }
    @Test fun actualWinUnlocksTheNextLevelWithItsStartingScore() {
        val game=GameEngine();game.start();win(game)
        assertEquals(CampaignCheckpoint(1,5000,1),game.completedMissionCheckpoint())
        game.returnToTitle();game.start()
        assertEquals(2,game.hud().levelNumber);assertEquals(5000,game.score)
        assertNull(game.completedMissionCheckpoint())
    }
    @Test fun fullCompletionKeepsLevelTenUntilExplicitNewGame() {
        val game=GameEngine();game.start()
        repeat(10){index->win(game);if(index<9)assertTrue(game.advanceLevel())}
        assertEquals(CampaignCheckpoint(9,45000,10),game.completedMissionCheckpoint())
        game.returnToTitle();game.start()
        assertEquals(10,game.hud().levelNumber);assertEquals(45000,game.score)
        game.startFresh();assertEquals(1,game.hud().levelNumber);assertEquals(0,game.score)
    }
}
