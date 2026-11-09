package com.spatialcommando.game.content

// SDK6.1.9: Attachment content keeps native Compose semantics; ECS accessibility APIs start in6.2.

import com.pico.spatial.core.container.SpatialViewContent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.core.math.EulerAngles
import com.spatialcommando.game.game.GameRuntime
import com.pico.spatial.ui.foundation.content.SpatialViewAttachments

internal fun bindGamePanels(attachments: SpatialViewAttachments, owner: Entity, width: Float, height: Float, depth: Float) {
    attachments.entity("hud")?.let {panel ->
        owner.addChild(panel)
        panel.components[TransformComponent::class.java]?.apply { position = Vector3(width * -0.43f, height * 0.36f, depth * 0.35f);eulerAngles=EulerAngles(0f,0f,0f) }
        GameRuntime.panelEntities["hud"] = panel
    }
    attachments.entity("enemyHud")?.let {panel ->
        owner.addChild(panel)
        panel.components[TransformComponent::class.java]?.apply {position=Vector3(width * 0.43f,height * 0.36f,depth * 0.35f);eulerAngles=EulerAngles(0f,0f,0f)}
        GameRuntime.panelEntities["enemyHud"]=panel
    }
    attachments.entity("title")?.let {panel ->
        owner.addChild(panel)
        panel.components[TransformComponent::class.java]?.apply { position = Vector3(0f, height * 0.47f, depth * 0.12f);eulerAngles=EulerAngles(12f,0f,0f) }
        GameRuntime.panelEntities["title"] = panel
    }
    attachments.entity("pause")?.let {panel ->
        owner.addChild(panel)
        panel.components[TransformComponent::class.java]?.apply { position = Vector3(0f, height * 0.47f, depth * 0.12f);eulerAngles=EulerAngles(12f,0f,0f) }
        GameRuntime.panelEntities["pause"] = panel
    }
    attachments.entity("result")?.let {panel ->
        owner.addChild(panel)
        panel.components[TransformComponent::class.java]?.apply { position = Vector3(0f, height * 0.47f, depth * 0.12f);eulerAngles=EulerAngles(12f,0f,0f) }
        GameRuntime.panelEntities["result"] = panel
    }
    attachments.entity("footer")?.let {panel ->
        owner.addChild(panel)
        panel.components[TransformComponent::class.java]?.apply { position = Vector3(width * 0.43f, height * -0.04f, depth * 0.35f); eulerAngles = EulerAngles(0f,0f,0f) }
        GameRuntime.panelEntities["footer"] = panel
    }
    attachments.entity("progress")?.let {panel ->
        owner.addChild(panel)
        panel.components[TransformComponent::class.java]?.apply { position = Vector3(0f, height * -0.385f, depth * 0.12f) }
        GameRuntime.panelEntities["progress"] = panel
    }
}

internal fun attachGameEnvironment(content: SpatialViewContent, owner: Entity) {
    content.addEntity(owner)
}
