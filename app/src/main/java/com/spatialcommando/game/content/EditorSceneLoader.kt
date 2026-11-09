package com.spatialcommando.game.content

import android.content.Context
import android.util.Log
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.resource.AssetBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class EditorGameScene(val loaded: Entity, val scope: Entity)

/** The runtime configuration is derived from the exported scenes sidecar and verified provenance. */
suspend fun loadEditorGameScene(context: Context, configAsset: String = "CommandoJungle.runtime.json"): EditorGameScene {
    val config = withContext(Dispatchers.IO) {
        context.assets.open(configAsset).bufferedReader().use { JSONObject(it.readText()) }
    }
    val sceneKey = config.getString("runtimeSceneKey")
    val names = config.getJSONArray("rootNames")
    val expected = (0 until names.length()).map { names.getString(it) }.sorted()
    require(expected.size == config.getInt("rootCount") && expected.isNotEmpty()) { "Incomplete Editor scene scope" }
    val bundle = withContext(Dispatchers.IO) { AssetBundle.load("asset://${config.getString("bundle")}") }
    val loaded = try { bundle.loadModelSuspend(sceneKey) } finally { bundle.close() }
    try {
        // SDK versions may return the manifest scope itself or an outer Entity.
        // Its runtime name is deliberately not inferred from the manifest's Root notation.
        val candidates = (listOf(loaded) + loaded.getChildren()).filter { child ->
            child.getChildren().map { it.getName() }.sorted() == expected
        }
        require(candidates.size == 1) {
            "Editor scope mismatch: returned children=${loaded.getChildren().map { it.getName() }} expected=$expected"
        }
        val scope = candidates.single()
        require(scope.getChildrenCount() == config.getInt("rootCount")) { "Editor root count mismatch" }
        Log.i("IronFront", "Loaded scene=$sceneKey manifestScope=${config.getString("scopePath")} runtimeScope=${scope.getName()} roots=${scope.getChildrenCount()}")
        return EditorGameScene(loaded, scope)
    } catch (e: Exception) {
        loaded.destroy()
        throw e
    }
}
