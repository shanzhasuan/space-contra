package com.spatialcommando.game.content

import android.content.Context
import android.util.Log
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.core.math.Vector3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Original downloaded GLBs are preserved; only instance transforms are normalized. */
suspend fun loadGeneratedModels(context: Context): Map<String, Entity> {
    val manifest = withContext(Dispatchers.IO) {
        context.assets.open("Models/manifest.json").bufferedReader().use { JSONObject(it.readText()) }
    }
    val result = linkedMapOf<String, Entity>()
    try {
        for (name in manifest.keys()) {
            val config = manifest.getJSONObject(name)
            val model = withContext(Dispatchers.IO) { Entity.load("asset://${config.getString("asset")}") }
            val anchor = Entity()
            anchor.setName(name)
            anchor.addChild(model)
            result[name] = anchor
            model.components[TransformComponent::class.java]!!.eulerAngles = EulerAngles(0f, config.optDouble("yawDegrees", 0.0).toFloat(), 0f)
            val bounds = model.getVisualBounds(anchor, recursive = true)
            require(bounds.halfExtent.y > 0f) { "Generated model has no height: $name" }
            val base = if(config.optBoolean("basePivot", true)) bounds.center.y - bounds.halfExtent.y else bounds.center.y
            model.components[TransformComponent::class.java]!!.apply {
                position = Vector3(position.x - bounds.center.x, position.y - base, position.z - bounds.center.z)
            }
            Log.i("IronFront", "Generated model loaded=$name asset=${config.getString("asset")} height=${bounds.halfExtent.y * 2}")
        }
        return result
    } catch (e: Exception) {
        result.values.forEach { it.destroy() }
        throw e
    }
}
