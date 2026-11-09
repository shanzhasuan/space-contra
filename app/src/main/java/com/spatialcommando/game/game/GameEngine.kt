package com.spatialcommando.game.game

import kotlin.math.*

enum class GameMode { TITLE, PLAYING, PAUSED, WON, LOST }
enum class Weapon(val title: String) { RIFLE("基础步枪"), SPREAD("散弹枪") }
data class InputFrame(val x: Float = 0f, val y: Float = 0f, val jump: Boolean = false, val shoot: Boolean = false, val switch: Boolean = false, val pause: Boolean = false, val back: Boolean = false, val prone: Boolean = false)
data class Platform(val x: Float, val width: Float, val y: Float, val ground: Boolean = false)
data class Enemy(val id: Int, val type: Int, var x: Float, var y: Float = 0f, var health: Int = 2, var cooldown: Float = 1.5f, var walk: Float = -1f, val originX: Float = x, val maxHealth: Int = health)
data class Bullet(val id: Int, var x: Float, var y: Float, val vx: Float, val vy: Float, val friendly: Boolean, var age: Float = 0f)
data class Pickup(val id: Int, val x: Float, val y: Float, var taken: Boolean = false)
data class Hud(val mode: GameMode, val lives: Int, val score: Int, val weapon: Weapon, val progress: Float, val bossHealth: Int, val bossActive: Boolean, val levelNumber: Int = 1, val totalLevels: Int = 10, val levelTitle: String = "", val bossMaxHealth: Int = 36, val bossName: String = "", val campaignComplete: Boolean = false, val targetName: String = "", val targetHealth: Int = 0, val targetMaxHealth: Int = 0, val maxLives: Int = 5, val prone: Boolean = false)

data class CampaignCheckpoint(val levelIndex: Int, val score: Int, val clearedLevels: Int)

/** Deterministic game rules in logical world units. Rendering converts these to meters. */
class GameEngine {
    var mode = GameMode.TITLE; private set
    var levelIndex = 0; private set
    val level: LevelDefinition get() = CampaignLevels.all[levelIndex]
    var missionRevision = 0; private set
    private var checkpointScore = 0
    var checkpointLevel = 0; private set
    val checkpointPoints get() = checkpointScore
    val campaignComplete get() = mode == GameMode.WON && levelIndex == CampaignLevels.all.lastIndex
    var lives = INITIAL_LIVES; private set
    var score = 0; private set
    var weapon = Weapon.RIFLE; private set
    var spreadUnlocked = false; private set
    var playerX = 0.5f; private set
    var playerY = 0f; private set
    var velocityY = 0f; private set
    var facing = 1f; private set
    var grounded = true; private set
    var crouching = false; private set
    var prone = false; private set
    var invulnerable = 1.5f; private set
    var cameraX = 0f; private set
    var elapsed = 0f; private set
    var aimAngle = 0f; private set
    private var fireCooldown = 0f
    private var nextId = 1
    private var previous = InputFrame()
    private var attackedTargetId: Int? = null
    private var attackedTargetAt = -10f
    private var accumulator = 0f
    private val sounds=ArrayDeque<GameSound>()
    private fun sound(effect: GameSound) {if(sounds.size<64)sounds.addLast(effect)}
    fun drainSoundEvents(): List<GameSound> {val result=sounds.toList();sounds.clear();return result}
    val platforms: List<Platform> get() = level.platforms
    val enemies = mutableListOf<Enemy>()
    val bullets = mutableListOf<Bullet>()
    val pickups = mutableListOf<Pickup>()
    init { populate() }

    fun start() { if(campaignComplete)startFresh() else startMission(checkpointLevel,INITIAL_LIVES,checkpointScore,Weapon.RIFLE,false) }
    fun startFresh() { checkpointLevel=0;checkpointScore=0;startMission(0,INITIAL_LIVES,0,Weapon.RIFLE,false) }
    fun restoreCheckpoint(index: Int, points: Int) {
        if(mode!=GameMode.TITLE)return
        checkpointLevel=index.coerceIn(0,CampaignLevels.all.lastIndex);checkpointScore=points.coerceAtLeast(0)
        levelIndex=checkpointLevel;score=checkpointScore;nextId=1;missionRevision++;populate()
    }
    fun restartLevel() { startMission(levelIndex, INITIAL_LIVES, checkpointScore, Weapon.RIFLE, false) }
    fun advanceLevel(): Boolean {
        if (mode != GameMode.WON || campaignComplete) return false
        startMission(levelIndex + 1, (lives + 1).coerceAtMost(INITIAL_LIVES), score, weapon, spreadUnlocked)
        return true
    }
    private fun startMission(index: Int, missionLives: Int, missionScore: Int, missionWeapon: Weapon, unlocked: Boolean) {
        levelIndex = index; checkpointLevel=index;checkpointScore = missionScore; missionRevision++
        mode = GameMode.PLAYING; lives = missionLives; score = missionScore; weapon = missionWeapon; spreadUnlocked = unlocked
        playerX = 0.5f; playerY = 0f; velocityY = 0f; facing = 1f; grounded = true; crouching = false;prone=false
        invulnerable = 1.5f; cameraX = 0f; elapsed = 0f; fireCooldown = 0f; accumulator = 0f; previous = InputFrame(); nextId = 1
        populate();sound(GameSound.START)
    }
    private fun populate() {
        enemies.clear(); bullets.clear(); pickups.clear();sounds.clear();attackedTargetId=null;attackedTargetAt=-10f
        fun onGround(x: Float): Float {
            if (platforms.any { it.ground && x >= it.x && x <= it.x + it.width }) return x
            return platforms.first { it.ground && it.x > x }.x + 0.3f
        }
        val soldiers = if (levelIndex == 0) listOf(4.2f, 8.0f, 10.8f, 14.1f, 16.2f)
            else (0 until level.soldierCount).map { 3.8f + it * (level.bossX - 6.3f) / (level.soldierCount - 1) }
        soldiers.forEach { enemies += Enemy(nextId++, 0, onGround(it), health = 2 + levelIndex / 3, cooldown = 1.5f / level.fireFactor) }
        val turrets = if (levelIndex == 0) listOf(5.7f, 11.8f, 15.5f)
            else (0 until level.turretCount).map { 5.3f + it * (level.bossX - 8f) / (level.turretCount - 1) }
        turrets.forEach { enemies += Enemy(nextId++, 1, onGround(it), health = 5 + levelIndex / 2, cooldown = 1.8f / level.fireFactor) }
        enemies += Enemy(nextId++, 2, level.bossX, health = level.bossHealth, cooldown = 2f / level.fireFactor)
        pickups += Pickup(nextId++, 3.2f, 0.12f)
        pickups += Pickup(nextId++, onGround(level.bossX * 0.53f), 0.12f)
    }
    /** Only a completed mission can unlock a saved continuation. */
    fun completedMissionCheckpoint(): CampaignCheckpoint? {
        if(mode != GameMode.WON) return null
        return CampaignCheckpoint((levelIndex+1).coerceAtMost(CampaignLevels.all.lastIndex),
            if(campaignComplete)checkpointScore else score,level.number)
    }
    fun returnToTitle() {
        val completed=completedMissionCheckpoint()
        val next=completed?.levelIndex ?: checkpointLevel
        val points=completed?.score ?: checkpointScore
        mode=GameMode.TITLE;prone=false;crouching=false;bullets.clear();previous=InputFrame();accumulator=0f;restoreCheckpoint(next,points)
    }
    fun pause() { if (mode == GameMode.PLAYING) { mode = GameMode.PAUSED; previous = InputFrame(); accumulator = 0f } }
    fun resume() { if (mode == GameMode.PAUSED) { mode = GameMode.PLAYING; previous = InputFrame() } }
    private fun currentTarget(): Enemy? {
        if(mode==GameMode.TITLE)return null
        val visible=enemies.filter {it.x>=cameraX-0.4f && it.x<=cameraX+7.7f}
        val attacked=visible.firstOrNull {it.id==attackedTargetId}
        if(attacked!=null && (attacked.health>0 || elapsed-attackedTargetAt<0.75f))return attacked
        val alive=visible.filter {it.health>0}
        val ahead=alive.filter {(it.x-playerX)*facing>=-0.3f}
        return (ahead.ifEmpty {alive}).minByOrNull {abs(it.x-playerX)}
    }
    fun hud(): Hud {
        val boss = enemies.firstOrNull { it.type == 2 && it.health > 0 }
        val target=currentTarget()
        val targetName=when(target?.type) {0->"拦路敌兵";1->"拦路炮台";2->level.bossName;else->""}
        return Hud(mode, lives, score, weapon, (playerX / level.bossX).coerceIn(0f, 1f), boss?.health ?: 0, playerX > level.bossX - 3.5f && boss != null, level.number, CampaignLevels.all.size, level.title, level.bossHealth, level.bossName, campaignComplete,
            targetName,target?.health?.coerceAtLeast(0)?:0,target?.maxHealth?:0,INITIAL_LIVES,prone)
    }

    fun update(delta: Float, input: InputFrame) {
        val jumpEdge = input.jump && !previous.jump
        val switchEdge = input.switch && !previous.switch
        val pauseEdge = input.pause && !previous.pause
        previous = input
        if (mode == GameMode.TITLE || mode == GameMode.WON || mode == GameMode.LOST) {
            if (jumpEdge) {
                when (mode) {
                    GameMode.WON -> if (campaignComplete) start() else advanceLevel()
                    GameMode.LOST -> restartLevel()
                    else -> start()
                }
            }
            return
        }
        if (pauseEdge) { if (mode == GameMode.PLAYING) pause() else resume(); previous = input; return }
        if (mode == GameMode.PAUSED) {
            if (jumpEdge) resume()
            previous = input; return
        }
        if (!delta.isFinite() || delta <= 0) return
        if (switchEdge && spreadUnlocked) weapon = if (weapon == Weapon.RIFLE) Weapon.SPREAD else Weapon.RIFLE
        val x = deadzone(input.x); val y = deadzone(input.y)
        prone = grounded && input.prone
        crouching = !prone && grounded && y < -0.55f && abs(x) < 0.25f
        if (abs(x) > 0.1f) facing = sign(x)
        aimAngle = if (x == 0f && y == 0f || crouching || prone) if (facing > 0) 0f else PI.toFloat() else round(atan2(y, x) / (PI.toFloat() / 4f)) * (PI.toFloat() / 4f)
        if (jumpEdge && grounded) { velocityY = 6.3f; grounded = false; crouching = false;prone=false;sound(GameSound.JUMP) }
        accumulator += delta.coerceAtMost(0.1f)
        while (accumulator >= STEP && mode == GameMode.PLAYING) {
            step(STEP, x, input.shoot); accumulator -= STEP
        }
    }
    private fun step(dt: Float, x: Float, shoot: Boolean) {
        elapsed += dt; fireCooldown -= dt; invulnerable = max(0f, invulnerable - dt)
        val lastY = playerY
        playerX = (playerX + (if (crouching || prone) 0f else x * 2.45f) * dt).coerceIn(0.18f, level.playerMaxX)
        velocityY -= 17f * dt; playerY += velocityY * dt; grounded = false
        if (velocityY <= 0f) {
            val floor = platforms.filter { playerX + 0.17f >= it.x && playerX - 0.17f <= it.x + it.width && lastY >= it.y - 0.02f && playerY <= it.y }.maxByOrNull { it.y }
            if (floor != null) { playerY = floor.y; velocityY = 0f; grounded = true }
        }
        cameraX = max(cameraX, (playerX - 2.1f).coerceIn(0f, level.cameraMaxX))
        if (playerY < -2f) { damage(true); return }
        if (shoot && fireCooldown <= 0) {
            sound(if(weapon==Weapon.SPREAD)GameSound.SPREAD_SHOT else GameSound.PLAYER_SHOT)
            val angles = if (weapon == Weapon.SPREAD) listOf(-0.22f, -0.11f, 0f, 0.11f, 0.22f) else listOf(0f)
            for (offset in angles) { val a = aimAngle + offset; bullets += Bullet(nextId++, playerX + cos(a) * (if(prone)0.48f else 0.23f), playerY + (if(prone)0.12f else if (crouching) 0.34f else 0.56f), cos(a) * 9.5f, sin(a) * 9.5f, true) }
            fireCooldown = if (weapon == Weapon.SPREAD) 0.24f else 0.16f
        }
        for (e in enemies) {
            if (e.health <= 0 || e.x < cameraX - 1 || e.x > cameraX + 8) continue
            if (e.type == 0) {
                val nextX = e.x + e.walk * 0.72f * level.walkFactor * dt
                if (abs(nextX - e.originX) > 1.15f || platforms.none { it.ground && nextX - 0.18f >= it.x && nextX + 0.18f <= it.x + it.width }) e.walk = -e.walk
                else e.x = nextX
            }
            e.cooldown -= dt
            if (e.cooldown <= 0 && abs(e.x - playerX) < 6 && (e.type != 2 || playerX > level.bossX - 3.6f)) {
                sound(GameSound.ENEMY_SHOT)
                val sourceY = e.y + if (e.type == 2) 0.75f else 0.55f
                val a = atan2(playerY + (if(crouching || prone)0.58f else 0.4f) - sourceY, playerX - e.x)
                val offsets = if (e.type == 2) if (levelIndex >= 5) listOf(-0.3f, -0.15f, 0f, 0.15f, 0.3f) else listOf(-0.18f, 0f, 0.18f) else listOf(0f)
                for (o in offsets) bullets += Bullet(nextId++, e.x, sourceY, cos(a + o) * (if (e.type == 2) 3.4f else 2.8f) * level.bulletFactor, sin(a + o) * (if (e.type == 2) 3.4f else 2.8f) * level.bulletFactor, false)
                e.cooldown = when (e.type) { 2 -> if (e.health <= level.bossHealth / 2) 1.05f else 1.55f; 1 -> 1.8f; else -> 2.0f } / level.fireFactor
            }
            if (invulnerable <= 0 && abs(e.x - playerX) < (if (e.type == 2) 0.65f else 0.28f) && playerY < (if (e.type == 2) 1.35f else 0.7f)) damage()
        }
        val iterator = bullets.iterator()
        while (iterator.hasNext()) {
            val b = iterator.next(); val oldX = b.x; val oldY = b.y
            b.x += b.vx * dt; b.y += b.vy * dt; b.age += dt
            var remove = b.age > 3f || b.x < cameraX - 1f || b.x > cameraX + 8f || b.y < -1 || b.y > 4
            if (!remove && b.friendly) {
                for (e in enemies) {
                    if (e.health <= 0) continue
                    val halfW = if (e.type == 2) 0.55f else if (e.type == 1) 0.35f else 0.22f
                    val h = if (e.type == 2) 1.3f else 0.78f
                    if (segmentHitsRect(oldX, oldY, b.x, b.y, e.x - halfW, e.y, e.x + halfW, e.y + h)) {
                        e.health--; remove = true;attackedTargetId=e.id;attackedTargetAt=elapsed
                        sound(if(e.health==0)GameSound.EXPLOSION else GameSound.HIT)
                        if (e.health == 0) { score += when (e.type) { 2 -> 5000; 1 -> 300; else -> 150 }; if (e.type == 2) {mode = GameMode.WON;sound(GameSound.CLEAR)} }
                        break
                    }
                }
            } else if (!remove && invulnerable <= 0 && segmentHitsRect(oldX, oldY, b.x, b.y, playerX - (if(prone)0.42f else 0.16f), playerY + (if(prone)0.02f else 0.05f), playerX + (if(prone)0.42f else 0.16f), playerY + if(prone)PRONE_HEIGHT else if (crouching) 0.52f else 0.78f)) {
                damage(); remove = true
            }
            if (remove) iterator.remove()
        }
        for (p in pickups) if (!p.taken && abs(playerX - p.x) < 0.35f && abs(playerY - p.y) < 0.55f) {
            sound(GameSound.PICKUP)
            p.taken = true; spreadUnlocked = true; weapon = Weapon.SPREAD; score += 100
        }
    }
    /** Receives a real hazard hit; immunity prevents repeated life loss during one collision. */
    fun damage(fall: Boolean = false) {
        if (mode != GameMode.PLAYING || !fall && invulnerable > 0f) return
        sound(GameSound.HURT)
        lives--; weapon = Weapon.RIFLE; spreadUnlocked = false; invulnerable = 2f
        if (lives <= 0) { mode = GameMode.LOST;sound(GameSound.GAME_OVER);return }
        if (fall) {
            val safe = platforms.filter { it.ground && it.x <= playerX }.lastOrNull() ?: platforms.first()
            playerX = (playerX - 1.2f).coerceIn(safe.x + 0.25f, safe.x + safe.width - 0.25f); playerY = safe.y; velocityY = 0f; grounded = true
        }
    }
    companion object {
        const val INITIAL_LIVES = 5
        const val PRONE_HEIGHT = 0.34f
        const val STEP = 1f / 120f
        fun deadzone(v: Float): Float = if (!v.isFinite() || abs(v) < 0.2f) 0f else ((abs(v).coerceAtMost(1f) - 0.2f) / 0.8f) * sign(v)
        /** Swept segment/AABB collision prevents fast bullets tunnelling through small actors. */
        fun segmentHitsRect(x0: Float, y0: Float, x1: Float, y1: Float, left: Float, bottom: Float, right: Float, top: Float): Boolean {
            var minT = 0f; var maxT = 1f
            for (axis in 0..1) {
                val origin = if (axis == 0) x0 else y0; val direction = if (axis == 0) x1 - x0 else y1 - y0
                val lo = if (axis == 0) left else bottom; val hi = if (axis == 0) right else top
                if (abs(direction) < 1e-7f) { if (origin < lo || origin > hi) return false }
                else { val a = (lo - origin) / direction; val b = (hi - origin) / direction; minT = max(minT, min(a,b)); maxT = min(maxT, max(a,b)); if (minT > maxT) return false }
            }
            return true
        }
    }
}
