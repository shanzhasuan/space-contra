package com.spatialcommando.game.game

/** Ten hand-tuned missions inspired by the classic environments, with original layouts. */
data class LevelDefinition(
    val number: Int,
    val title: String,
    val theme: String,
    val bossName: String,
    val bossX: Float,
    val bossHealth: Int,
    val soldierCount: Int,
    val turretCount: Int,
    val fireFactor: Float,
    val bulletFactor: Float,
    val walkFactor: Float,
    val platforms: List<Platform>,
) {
    val playerMaxX get() = bossX + 1.5f
    val cameraMaxX get() = playerMaxX - 6.7f
}

object CampaignLevels {
    private val titles = listOf(
        "丛林前线", "断桥突围", "地下堡垒", "瀑布峡谷", "冰原哨站",
        "能源中枢", "重装机库", "熔炉地带", "异变实验室", "异形核心"
    )
    private val themes = listOf("jungle", "bridge", "bunker", "waterfall", "snow", "energy", "hangar", "furnace", "laboratory", "alien")
    private val bosses = listOf(
        "守门机甲", "桥头重炮", "堡垒守卫", "峡谷猎手", "冰原坦克",
        "能源守护者", "机库步行机", "熔炉装甲", "实验体守卫", "核心主宰"
    )
    val all: List<LevelDefinition> = (0 until 10).map { rank ->
        val bossX = 19f + rank * 1.6f
        val firstEnd = 6.5f + rank * 0.35f
        val gap = 0.8f + rank * 0.035f
        val secondStart = firstEnd + gap
        val secondEnd = 12.5f + rank * 0.8f
        val thirdStart = secondEnd + 0.7f + rank * 0.025f
        val platforms = listOf(
            Platform(0f, firstEnd, 0f, true),
            Platform(secondStart, secondEnd - secondStart, 0f, true),
            Platform(thirdStart, bossX + 2.2f - thirdStart, 0f, true),
            Platform(3f + rank * 0.2f, 1.9f - rank * 0.04f, 0.85f + (rank % 3) * 0.12f),
            Platform(secondStart + 1.4f, 1.7f - rank * 0.025f, 1.05f + (rank % 2) * 0.07f),
            Platform(thirdStart + 1f, 1.8f - rank * 0.035f, 0.85f + (rank % 4) * 0.09f),
        )
        LevelDefinition(rank + 1, titles[rank], themes[rank], bosses[rank], bossX,
            36 + rank * 8, 5 + rank, 3 + rank / 2, 1f + rank * 0.11f,
            1f + rank * 0.065f, 1f + rank * 0.07f, platforms)
    }
}
