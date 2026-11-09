package com.spatialcommando.game.content

import com.pico.spatial.core.ecs.Entity

/** Entity.enabled is local to one node; every descendant needs its own visibility. */
internal class EntityTreeVisibility(root: Entity, preserveDisabledNodes: Boolean = false) {
    private val nodes = buildList {
        fun visit(entity: Entity) {
            add(entity)
            entity.getChildren().forEach(::visit)
        }
        visit(root)
    }
    private val enabledWhenVisible = nodes.map { !preserveDisabledNodes || it.enabled }
    private var visible: Boolean? = null

    fun setVisible(value: Boolean) {
        if (visible == value) return
        nodes.forEachIndexed { index, node -> node.enabled = value && enabledWhenVisible[index] }
        visible = value
    }
}
