package com.spatialcommando.game.content

import com.pico.spatial.core.container.SpatialViewContent
import com.pico.spatial.core.ecs.AccessibilityComponent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.core.math.EulerAngles
import com.spatialcommando.game.game.GameRuntime
import com.pico.spatial.ui.foundation.content.SpatialViewAttachments
import com.pico.spatial.ui.foundation.content.setAccessibilityActions

internal fun bindBattlefieldAccessibility(content: SpatialViewContent, owner: Entity) {
    owner.components.set(AccessibilityComponent(label="闯关战场",value="等待行动开始").apply {isAccessibilityElement=true})
    // One actual scene owner describes the non-interactive actor/scenery hierarchy.
    // Gameplay remains physical-controller input; no reactive AI gameplay action is published.
    content.setAccessibilityActions(entity=owner,semanticId="ironfront.battlefield") {}
}

internal fun bindPanelAccessibility(content: SpatialViewContent, attachments: SpatialViewAttachments, owner: Entity, width: Float, height: Float, depth: Float) {
    attachments.entity("hud")?.let {panel ->
        owner.addChild(panel)
        panel.components[TransformComponent::class.java]?.apply { position = Vector3(width * -0.43f, height * 0.36f, depth * 0.35f);eulerAngles=EulerAngles(0f,0f,0f) }
        GameRuntime.panelEntities["hud"] = panel
        panel.components.set(AccessibilityComponent(label="左侧玩家状态",value="生命、得分、武器和关卡").apply {isAccessibilityElement=true})
        content.setAccessibilityActions(entity=panel,semanticId="ironfront.hud") {}
    }
    attachments.entity("enemyHud")?.let {panel ->
        owner.addChild(panel)
        panel.components[TransformComponent::class.java]?.apply {position=Vector3(width * 0.43f,height * 0.36f,depth * 0.35f);eulerAngles=EulerAngles(0f,0f,0f)}
        GameRuntime.panelEntities["enemyHud"]=panel
        panel.components.set(AccessibilityComponent(label="右侧敌人血量",value="等待开始").apply {isAccessibilityElement=true})
        content.setAccessibilityActions(entity=panel,semanticId="ironfront.enemy-hud") {}
    }
    attachments.entity("title")?.let {panel ->
        owner.addChild(panel)
        panel.components[TransformComponent::class.java]?.apply { position = Vector3(0f, height * 0.47f, depth * 0.12f);eulerAngles=EulerAngles(12f,0f,0f) }
        GameRuntime.panelEntities["title"] = panel
        panel.components.set(AccessibilityComponent(label="空间魂斗罗标题",value="开始行动和手柄说明").apply {isAccessibilityElement=true})
        content.setAccessibilityActions(entity=panel,semanticId="ironfront.title") {}
    }
    attachments.entity("pause")?.let {panel ->
        owner.addChild(panel)
        panel.components[TransformComponent::class.java]?.apply { position = Vector3(0f, height * 0.47f, depth * 0.12f);eulerAngles=EulerAngles(12f,0f,0f) }
        GameRuntime.panelEntities["pause"] = panel
        panel.components.set(AccessibilityComponent(label="行动暂停",value="继续游戏或重试本关").apply {isAccessibilityElement=true})
        content.setAccessibilityActions(entity=panel,semanticId="ironfront.pause") {}
    }
    attachments.entity("result")?.let {panel ->
        owner.addChild(panel)
        panel.components[TransformComponent::class.java]?.apply { position = Vector3(0f, height * 0.47f, depth * 0.12f);eulerAngles=EulerAngles(12f,0f,0f) }
        GameRuntime.panelEntities["result"] = panel
        panel.components.set(AccessibilityComponent(label="行动结果",value="下一关或重新挑战").apply {isAccessibilityElement=true})
        content.setAccessibilityActions(entity=panel,semanticId="ironfront.result") {}
    }
    attachments.entity("footer")?.let {panel ->
        owner.addChild(panel)
        panel.components[TransformComponent::class.java]?.apply { position = Vector3(width * 0.43f, height * -0.04f, depth * 0.35f); eulerAngles = EulerAngles(0f,0f,0f) }
        GameRuntime.panelEntities["footer"] = panel
        panel.components.set(AccessibilityComponent(label="右侧操作指南",value="移动、蹲伏、趴下、跳跃、射击和换武器").apply {isAccessibilityElement=true})
        content.setAccessibilityActions(entity=panel,semanticId="ironfront.footer") {}
    }
    attachments.entity("progress")?.let {panel ->
        owner.addChild(panel)
        panel.components[TransformComponent::class.java]?.apply { position = Vector3(0f, height * -0.385f, depth * 0.12f) }
        GameRuntime.panelEntities["progress"] = panel
        panel.components.set(AccessibilityComponent(label="关卡进度",value="丛林前线推进进度").apply {isAccessibilityElement=true})
        content.setAccessibilityActions(entity=panel,semanticId="ironfront.progress") {}
    }
}

internal fun bindEnvironmentAccessibility(content: SpatialViewContent, owner: Entity) {
    content.addEntity(owner)
    owner.components.set(AccessibilityComponent(label="关卡全景环境",value="环绕远山、森林、遗迹和天空").apply {isAccessibilityElement=true})
    content.setAccessibilityActions(entity=owner,semanticId="ironfront.environment") {}
}
