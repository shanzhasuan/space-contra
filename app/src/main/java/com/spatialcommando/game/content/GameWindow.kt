package com.spatialcommando.game.content

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pico.spatial.ui.design.*
import com.spatialcommando.game.R
import com.spatialcommando.game.game.*

private val SkyCardColor = Color(0xFF375055) // design-style: fixed-figma-color user sky palette sampled at panorama (2048,512)
const val CONTROLLER_STAGE = "IronFrontControllerStage"

@Composable
fun GameWindow() {
    ControllerInputLifecycle(true)
    DisposableEffect(Unit) {
        GameRuntime.fullSpaceReady = true
        com.pico.spatial.core.ecs.System.register(CommandoGameSystem::class.java)
        onDispose {
            com.pico.spatial.core.ecs.System.unregister(CommandoGameSystem::class.java)
            GameRuntime.dispose(); GameRuntime.fullSpaceReady=false
        }
    }
    CompositionLocalProvider(LocalContentColor provides PicoTheme.colorScheme.labelPrimaryLight) {
        IronFrontScene()
    }
}

@Composable
private fun MenuCardContent(modifier: Modifier, padding: Dp = 24.dp, spacing: Dp = 12.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.padding(padding),verticalArrangement=Arrangement.spacedBy(spacing),content=content)
}

@Composable
fun GameHud() {
    val hud=GameRuntime.hud
    // design-style: design-surface hud-panel
    Column(Modifier.size(232.dp,168.dp).testTag("player-status-hud").semantics(mergeDescendants=true) {
        contentDescription="玩家状态"
        stateDescription="生命 ${hud.lives}/${hud.maxLives}，得分 ${hud.score}，${hud.levelNumber}/10 ${hud.levelTitle}"
    }.clip(RoundedCornerShape(20.dp))
        .background(SkyCardColor).padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text("玩家状态",style=PicoTheme.typography.titleMedium)
        Text("生命 ${hud.lives.coerceAtLeast(0)}/${hud.maxLives}  ${"♥ ".repeat(hud.lives.coerceAtLeast(0))}",style=PicoTheme.typography.bodyMedium)
        Text("得分 ${hud.score.toString().padStart(6,'0')}",style=PicoTheme.typography.bodyMedium)
        Text("武器 ${hud.weapon.title}${if(hud.prone)" · 趴下" else ""}",style=PicoTheme.typography.bodyMedium)
        Text("${hud.levelNumber.toString().padStart(2,'0')}/10 ${hud.levelTitle}",style=PicoTheme.typography.bodySmall)
    }
}

@Composable
fun EnemyHud() {
    val hud=GameRuntime.hud
    // design-style: design-surface enemy-hud-panel
    Column(Modifier.size(232.dp,168.dp).testTag("enemy-status-hud").semantics(mergeDescendants=true) {
        contentDescription="敌人状态"
        stateDescription=if(hud.targetMaxHealth>0)"${hud.targetName}，血量 ${hud.targetHealth}/${hud.targetMaxHealth}" else "附近暂无目标"
    }.clip(RoundedCornerShape(20.dp))
        .background(SkyCardColor).padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("敌人状态",style=PicoTheme.typography.titleMedium)
        if(hud.targetMaxHealth>0) {
            Text(hud.targetName,style=PicoTheme.typography.bodyMedium)
            Text("血量 ${hud.targetHealth} / ${hud.targetMaxHealth}",style=PicoTheme.typography.bodyLarge)
            LinearProgressIndicator(progress={hud.targetHealth.toFloat()/hud.targetMaxHealth},
                modifier=Modifier.fillMaxWidth().height(6.dp),
                colors=LinearProgressDefaults.linearProgressColors(
                    indicatorColor=PicoTheme.colorScheme.labelPrimaryLight,
                    backgroundColor=PicoTheme.colorScheme.labelPrimaryLight.copy(alpha=0.22f)))
            if(hud.targetHealth==0)Text("已击破",style=PicoTheme.typography.bodySmall)
        } else {
            Text(if(hud.mode==GameMode.TITLE)"进入关卡后显示" else "附近暂无目标",style=PicoTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun TitlePanel() {
    // design-style: design-surface menu-panel
    MenuCardContent(Modifier.width(340.dp).testTag("title-panel").clip(RoundedCornerShape(24.dp))
        .background(SkyCardColor),padding=16.dp,spacing=8.dp) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Image(painterResource(R.drawable.game_emblem),contentDescription="空间魂斗罗徽章",modifier=Modifier.size(36.dp))
            Column {
                // Reviewed brand heading: .scratch/design-spec.json node game-title.
                Text("空间魂斗罗",style=PicoTheme.typography.titleLarge.copy(fontSize=24.sp,fontWeight=FontWeight.Bold))
                Text("SPATIAL CONTRA · 10 关闯关",style=PicoTheme.typography.bodySmall)
            }
        }
        Text("操作说明见右侧指南。",style=PicoTheme.typography.bodySmall)
        if(!GameRuntime.sceneReady)Text(GameRuntime.loadingText,style=PicoTheme.typography.bodySmall)
        if(!GameRuntime.controlsReady)Text(GameRuntime.controlText,style=PicoTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={GameRuntime.newCampaign()},enabled=GameRuntime.sceneReady,size=ButtonDefaults.Max,
                modifier=Modifier.weight(1f).testTag("new-campaign")) {Text("新游戏",style=PicoTheme.typography.bodyLarge)}
            Button(onClick={GameRuntime.start()},enabled=GameRuntime.sceneReady,size=ButtonDefaults.Max,
                modifier=Modifier.weight(1f).testTag("start-game")) {Text("继续游戏",style=PicoTheme.typography.bodyLarge)}
        }
    }
}

@Composable
fun PausePanel() {
    // design-style: design-surface pause-panel
    MenuCardContent(Modifier.width(400.dp).testTag("pause-panel").clip(RoundedCornerShape(24.dp))
        .background(SkyCardColor)) {
        Text("行动暂停",style=PicoTheme.typography.titleLarge)
        Text("进度已保留，可继续或重试本关。",style=PicoTheme.typography.bodyMedium)
        if(!GameRuntime.controlsReady)Text(GameRuntime.controlText,style=PicoTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp,Alignment.End)) {
            Button(onClick={GameRuntime.restart()},modifier=Modifier.testTag("restart-game")){Text("重试本关")}
            Button(onClick={GameRuntime.resume()},modifier=Modifier.testTag("resume-game")){Text("继续游戏")}
        }
    }
}

@Composable
fun ResultPanel() {
    val hud=GameRuntime.hud
    if(hud.mode==GameMode.LOST) {
        // design-style: design-surface defeat-panel
        Column(Modifier.size(240.dp,104.dp).testTag("defeat-panel").clip(RoundedCornerShape(20.dp))
            .background(SkyCardColor).padding(16.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
            Button(onClick={GameRuntime.restart()},size=ButtonDefaults.Max,modifier=Modifier.fillMaxWidth().testTag("retry-game")) {
                Text("\u91cd\u65b0\u6311\u6218",style=PicoTheme.typography.bodyLarge)
            }
        }
        return
    }
    // design-style: design-surface result-panel
    MenuCardContent(Modifier.width(400.dp).testTag("result-panel").clip(RoundedCornerShape(24.dp))
        .background(SkyCardColor)) {
        Text(if(hud.campaignComplete)"10 关全部通关" else if(hud.mode==GameMode.WON)"据点已攻破" else "行动失败",style=PicoTheme.typography.titleLarge)
        Text("得分 ${hud.score.toString().padStart(6,'0')}",style=PicoTheme.typography.titleMedium)
        Text(if(hud.campaignComplete)"全部任务完成。" else if(hud.mode==GameMode.WON)"${hud.levelNumber}/10 ${hud.levelTitle} 已攻破。" else "可重新挑战当前关卡。",style=PicoTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp,Alignment.End)) {
            Button(onClick={
                if(hud.mode==GameMode.WON&&!hud.campaignComplete)GameRuntime.nextLevel()
                else if(hud.campaignComplete)GameRuntime.start() else GameRuntime.restart()
            },modifier=Modifier.testTag("retry-game")){Text(if(hud.mode==GameMode.WON&&!hud.campaignComplete)"下一关" else "再玩一遍")}
        }
    }
}

@Composable
fun ControlsHud() {
    val mode=GameRuntime.hud.mode
    var expanded by remember(mode==GameMode.TITLE) {mutableStateOf(mode==GameMode.TITLE)}
    // design-style: design-surface footer-content
    Column(Modifier.size(232.dp,if(expanded)256.dp else 76.dp).testTag("controls-hud").clip(RoundedCornerShape(20.dp))
        .background(SkyCardColor).padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        if(!expanded) {
            Button(onClick={expanded=true},modifier=Modifier.fillMaxWidth().testTag("show-controls")) {Text("\u64cd\u4f5c\u6307\u5357")}
            return@Column
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            Text("\u64cd\u4f5c\u6307\u5357",style=PicoTheme.typography.titleMedium)
            Button(onClick={expanded=false},modifier=Modifier.testTag("hide-controls")) {Text("\u6536\u8d77")}
        }
        Text("\u5de6\u6447\u6746 \u79fb\u52a8 \u00b7 \u7784\u51c6 \u00b7 \u8e72\u4f0f",style=PicoTheme.typography.bodySmall)
        Text("A \u8df3\u8dc3 / \u83dc\u5355\u786e\u8ba4",style=PicoTheme.typography.bodySmall)
        Text("B / \u6273\u673a \u5c04\u51fb",style=PicoTheme.typography.bodySmall)
        Text("X \u5207\u6362\u6b66\u5668",style=PicoTheme.typography.bodySmall)
        Text("\u53f3\u6447\u6746\u2193 \u8db4\u4e0b / \u677e\u5f00\u7ad9\u8d77",style=PicoTheme.typography.bodySmall)
        Text("\u83dc\u5355\u7528\u6307\u9488\u70b9\u51fb\u6309\u94ae",style=PicoTheme.typography.bodySmall)
    }
}
