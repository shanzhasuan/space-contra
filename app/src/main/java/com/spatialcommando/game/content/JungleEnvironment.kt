package com.spatialcommando.game.content

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.DirectionalLightComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.ModelEntity
import com.pico.spatial.core.ecs.StageEnvironmentLightingComponent
import com.pico.spatial.core.ecs.ImageBasedLightSource
import com.pico.spatial.core.ecs.resource.ImageBasedLightingGenerationOption
import com.pico.spatial.core.ecs.resource.MaterialCullingMode
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.SphereOption
import com.pico.spatial.core.ecs.resource.PrimitiveSphereTopology
import com.pico.spatial.core.ecs.resource.MaterialTextureParameter
import com.pico.spatial.core.ecs.resource.TextureCreateOption
import com.pico.spatial.core.ecs.resource.TextureResource
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Owns only the separately authored Editor sky and its generated environment lighting. */
class JungleEnvironment private constructor(
    val root: Entity,
    private val panorama: TextureResource,
    private val lighting: TextureResource,
    private val material: UnlitMaterial,
    private val skyMesh: MeshResource,
    private val variants: Map<String, Pair<TextureResource, TextureResource>>,
) {
    private var closed = false
    private var currentTheme = "jungle"
    fun applyTheme(theme: String) {
        if(closed || currentTheme == theme) return
        val textures = variants[theme] ?: (panorama to lighting)
        material.setBaseColorTexture(textures.first)
        root.components.set(StageEnvironmentLightingComponent(ImageBasedLightSource.Single(textures.second), if(theme in setOf("alien","furnace","energy","laboratory")) 1f else 2f))
        val key = root.getChildren().singleOrNull { it.getName()=="JungleKeyLight" }
        val fill = root.getChildren().singleOrNull { it.getName()=="JungleFillLight" }
        key?.components?.get(DirectionalLightComponent::class.java)?.color = when(theme) {
            "furnace" -> Color4(1f,0.75f,0.5f,1f)
            "alien" -> Color4(0.85f,0.72f,1f,1f)
            "snow","waterfall","energy","laboratory" -> Color4(0.82f,0.94f,1f,1f)
            else -> Color4(1f,0.95f,0.85f,1f)
        }
        fill?.components?.get(DirectionalLightComponent::class.java)?.color = when(theme) {
            "furnace" -> Color4(1f,0.45f,0.22f,1f)
            "alien","laboratory" -> Color4(0.6f,1f,0.8f,1f)
            else -> Color4(0.65f,0.8f,1f,1f)
        }
        currentTheme = theme
        Log.i("IronFront", "Environment theme=$theme")
    }
    fun close() {
        if (closed) return
        closed = true
        root.destroy()
        material.close()
        skyMesh.close()
        lighting.close()
        panorama.close()
        variants.values.forEach { (color,ibl) -> color.close(); ibl.close() }
    }

    companion object {
        suspend fun load(context: Context): JungleEnvironment {
            val scene = loadEditorGameScene(context, "JungleEnvironment.runtime.json")
            var panorama: TextureResource? = null
            var lighting: TextureResource? = null
            var material: UnlitMaterial? = null
            var skyMesh: MeshResource? = null
            val variants = linkedMapOf<String, Pair<TextureResource,TextureResource>>()
            try {
                panorama = withContext(Dispatchers.IO) {
                    val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888; inSampleSize = 4 }
                    val bitmap = context.assets.open("Environment/jungle_panorama.jpg").use {
                        requireNotNull(BitmapFactory.decodeStream(it, null, options)) { "Panorama bitmap decode failed" }
                    }
                    try { TextureResource(bitmap, TextureCreateOption()) } finally { bitmap.recycle() }
                }
                lighting = withContext(Dispatchers.IO) {
                    TextureResource.load("Environment/jungle_lighting.jpg", option = ImageBasedLightingGenerationOption().apply {
                        desiredCubemapFaceSize = 128L
                    })
                }
                require(panorama.valid && lighting.valid) { "Panorama or lighting texture is invalid" }
                panorama.toGlobal(); lighting.toGlobal()
                for(theme in listOf("bridge","bunker","waterfall","snow","energy","hangar","furnace","laboratory","alien")) {
                    val color = withContext(Dispatchers.IO) {
                        val bitmap = context.assets.open("Environment/$theme.jpg").use { requireNotNull(BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply {inSampleSize=2;inPreferredConfig=Bitmap.Config.ARGB_8888})) }
                        try { TextureResource(bitmap, TextureCreateOption()) } finally { bitmap.recycle() }
                    }
                    color.toGlobal()
                    try {
                        val ibl = withContext(Dispatchers.IO) {
                            TextureResource.load("Environment/${theme}_lighting.jpg", option=ImageBasedLightingGenerationOption().apply { desiredCubemapFaceSize=64L })
                        }
                        ibl.toGlobal(); variants[theme] = color to ibl
                    } catch(e: Exception) { color.close(); throw e }
                }
                val mesh = MeshResource.createSphere(SphereOption(8f).setTopology(PrimitiveSphereTopology.LATLON))
                mesh.toGlobal(); skyMesh = mesh
                // Preserve the Editor sphere's transform, replacing its legacy analytic mesh UVs.
                val skyMaterial = UnlitMaterial.create().apply {
                    setBaseColorTexture(panorama)
                    setBaseColor(Color4(1f, 1f, 1f, 1f))
                    setCullingMode(MaterialCullingMode.NONE)
                    setApplyToneMapping(true)
                    setDepthTest(true)
                    setDepthWrite(true)
                }
                skyMaterial.toGlobal()
                material = skyMaterial
                // The packed analytic Sphere remains as an Editor authoring reference.
                // Its runtime replacement has explicit panorama UVs and normal SDK depth behavior.
                scene.scope.getChildren().forEach { it.enabled = false }
                scene.loaded.setName("JunglePanoramaEnvironment")
                scene.loaded.enabled = true
                val sky = ModelEntity(mesh, skyMaterial)
                sky.setName("JunglePanoramaDome")
                sky.components[TransformComponent::class.java]!!.position = Vector3(0f, 1.45f, 0f)
                scene.loaded.addChild(sky)
                val meshCount = 1
                scene.loaded.components.set(StageEnvironmentLightingComponent(ImageBasedLightSource.Single(lighting), 2f))
                val keyLight = Entity()
                keyLight.setName("JungleKeyLight")
                keyLight.components.set(DirectionalLightComponent(Color4(1f, 0.95f, 0.85f, 1f), 800f, false))
                keyLight.components[TransformComponent::class.java]!!.eulerAngles = EulerAngles(-15f, 0f, 0f)
                scene.loaded.addChild(keyLight)
                val fillLight = Entity()
                fillLight.setName("JungleFillLight")
                fillLight.components.set(DirectionalLightComponent(Color4(0.65f, 0.8f, 1f, 1f), 300f, false))
                fillLight.components[TransformComponent::class.java]!!.eulerAngles = EulerAngles(0f, 180f, 0f)
                scene.loaded.addChild(fillLight)
                Log.i("IronFront", "Jungle sky ready: panorama=1024x512 radius=8m meshes=$meshCount IBL=128")
                return JungleEnvironment(scene.loaded, panorama, lighting, skyMaterial, mesh, variants)
            } catch (e: Exception) {
                scene.loaded.destroy()
                material?.close(); skyMesh?.close(); lighting?.close(); panorama?.close()
                variants.values.forEach { (color,ibl) -> color.close(); ibl.close() }
                throw e
            }
        }
    }
}
