package com.spatialcommando.game.content

import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.core.math.Vector3
import com.spatialcommando.game.game.*
import kotlin.math.*

/** Displays only Editor-authored geometry. Source scope must be manifest-verified by the caller. */
class GameModelRenderer(
    override val root: Entity,
    private val loaded: Entity,
    private val scope: Entity,
    private val width: Float,
    private val height: Float,
    private val depth: Float,
    private val generatedModels: Map<String, Entity> = emptyMap(),
    private val applyEnvironment: (String) -> Unit = {},
) : SceneRenderTarget {
    private val unit = width * 0.94f / 7.3f
    private val floorY = -height * 0.28f
    private val active = Entity()
    private val reference = Entity()
    private val visualEntities = mutableListOf<Entity>()
    private val visibility = mutableMapOf<Entity, EntityTreeVisibility>()
    private var staticMissionEntities = emptyList<Entity>()
    private lateinit var referenceVisibility: EntityTreeVisibility
    private val enemyModels = mutableMapOf<Int, Entity>()
    private val pickupModels = mutableMapOf<Int, Entity>()
    private val groundModels = mutableListOf<Pair<Entity,Entity>>()
    private val platformModels = mutableListOf<Pair<Entity,Entity>>()
    private val friendlyPool = ArrayDeque<Entity>()
    private val hostilePool = ArrayDeque<Entity>()
    private val bulletModels = mutableMapOf<Int,Pair<Entity,Boolean>>()
    private val baseScales = mutableMapOf<Entity,Vector3>()
    private var previousX = 0.5f
    private var closed = false
    private var missionRevision = -1
    private var lastTerrainTheme = ""
    private val archetypes=Entity()
    private var heroAnimation: SkeletalActor?=null
    private val enemyAnimations=mutableMapOf<Int,SkeletalActor>()
    private var moving=false
    private var lastAnimatedTime=0f
    private var retainedHeroScale: Vector3?=null
    private lateinit var player: Entity
    private var leftLeg: Entity? = null
    private var rightLeg: Entity? = null
    private var rifle: Entity? = null

    init {
        reference.setName("JungleReference")
        root.addChild(reference); reference.addChild(loaded)
        reference.components[TransformComponent::class.java]!!.apply {
            position=Vector3(0f,floorY,0f)
            val f=minOf(width*0.94f/5.5f,height*0.72f/1.5f,depth*0.88f/0.937f)
            scaleVector=Vector3(f,f,f)
        }
        archetypes.setName("GeneratedArchetypes")
        root.addChild(archetypes)
        generatedModels.values.forEach { archetypes.addChild(it) }
        EntityTreeVisibility(archetypes).setVisible(false)
        for((name,x,modelHeight) in listOf(Triple("Player",-2.1f,0.8f),Triple("Enemy",0.2f,0.78f),Triple("Turret",1.2f,0.78f),Triple("Guardian",2.1f,1.3f),Triple("Supply",-0.8f,0.25f))) {
            generatedModels[name]?.let { model ->
                setVisible(direct(scope,name),false)
                heightSized(model,"Title$name",modelHeight,reference).components[TransformComponent::class.java]!!.apply {
                    position=Vector3(x,if(name=="Supply")0.3f else 0f,0.12f)
                    eulerAngles=EulerAngles(0f,if(name in setOf("Enemy","Turret","Guardian"))180f else 0f,0f)
                }
            }
        }
        generatedModels["Background"]?.let { model ->
            setVisible(direct(scope,"Background"),false)
            for(i in 0 until 5) heightSized(model,"TitleForest_$i",1.5f,reference).components[TransformComponent::class.java]!!.position=Vector3(-2.2f+i*1.1f,0f,-0.4f)
        }
        generatedModels["Mountains"]?.let { model ->
            setVisible(direct(scope,"Mountains"),false)
            for(i in 0 until 2) heightSized(model,"TitleRuins_$i",1.2f,reference).components[TransformComponent::class.java]!!.position=Vector3(-1.4f+i*2.8f,0f,-0.7f)
        }
        active.setName("ActiveMission")
        root.addChild(active)
        active.components[TransformComponent::class.java]!!.position=Vector3(0f,floorY,0f)
        val backdrop=prototype("Backdrop")
        // The immersive panorama replaces the old rectangular backdrop.
        setVisible(backdrop,false)
        buildMission(GameRuntime.engine)
        referenceVisibility=EntityTreeVisibility(reference,preserveDisabledNodes=true)
        active.enabled=false
        active.getChildren().forEach {setVisible(it,false)}
    }

    private fun buildMission(engine: GameEngine) {
        val liveHero=generatedModels["PlayerLive"]
        player=if(liveHero!=null) {
            active.addChild(liveHero);liveHero.setName("PlayerActor");setVisible(liveHero,true)
            if(retainedHeroScale==null) {
                val b=liveHero.getVisualBounds(active,recursive=true)
                val f=unit*0.8f/(b.halfExtent.y*2f)
                val scale=liveHero.components[TransformComponent::class.java]!!.scaleVector
                retainedHeroScale=Vector3(scale.x*f,scale.y*f,scale.z*f)
            }
            liveHero.components[TransformComponent::class.java]!!.scaleVector=retainedHeroScale!!
            baseScales[liveHero]=liveHero.components[TransformComponent::class.java]!!.scaleVector
            liveHero
        } else heightSized(prototype("Player"),"PlayerActor",unit*0.8f)
        heroAnimation=SkeletalActor.create(player,required=liveHero!=null)
        if(liveHero!=null)require(heroAnimation?.supportsProne==true){"Hero must include prone clip"}
        leftLeg=player.getChildren().singleOrNull {it.getName()=="LegLeft"}
        rightLeg=player.getChildren().singleOrNull {it.getName()=="LegRight"}
        rifle=player.getChildren().singleOrNull {it.getName()=="Rifle"}
        for(e in engine.enemies) {
            val name=when(e.type){2->"Guardian";1->"Turret";else->"Enemy"}
            enemyModels[e.id]=heightSized(prototype(name),"Enemy_${e.id}",unit*if(e.type==2)1.3f else 0.78f)
            if(e.type==0)SkeletalActor.create(enemyModels[e.id]!!)?.let {enemyAnimations[e.id]=it}
        }
        for(p in engine.pickups) pickupModels[p.id]=heightSized(prototype("Supply"),"Pickup_${p.id}",unit*0.25f)
        val terrain=prototype("Terrain")
        val ground=direct(terrain,"Ground_0");val moss=direct(terrain,"Moss_0")
        val rock=direct(terrain,"Platform_0");val support=direct(terrain,"Support_0")
        for((i,p) in engine.platforms.filter{it.ground}.withIndex()) {
            groundModels += sized(ground,"MissionGround_$i",p.width*unit,unit*0.65f,unit*1.7f) to
                sized(moss,"MissionMoss_$i",p.width*unit,unit*0.045f,unit*1.7f)
        }
        for((i,p) in engine.platforms.filter{!it.ground}.withIndex()) {
            platformModels += sized(rock,"MissionPlatform_$i",p.width*unit,unit*0.18f,unit*1.6f) to
                sized(support,"MissionSupport_$i",unit*0.22f,(p.y-0.18f)*unit,unit*0.20f)
        }
        buildScenery(engine)
        val projectiles=prototype("Projectiles")
        val yellow=direct(projectiles,"PlayerBullet");val red=direct(projectiles,"EnemyBullet")
        repeat(96) {friendlyPool.addLast(sized(yellow,"FriendlyPool_$it",unit*0.14f,unit*0.035f,unit*0.035f).also {e->setVisible(e,false)})}
        repeat(48) {hostilePool.addLast(sized(red,"HostilePool_$it",unit*0.075f,unit*0.06f,unit*0.06f).also {e->setVisible(e,false)})}
        val dynamic = (enemyModels.values + pickupModels.values + friendlyPool + hostilePool + player).toSet()
        staticMissionEntities = active.getChildren().filter { it !in dynamic }
        missionRevision = engine.missionRevision
    }

    private fun buildScenery(engine: GameEngine) {
        val outdoor=engine.level.theme in setOf("jungle","bridge","waterfall","snow")
        if(outdoor && generatedModels.containsKey("Background")) {
            for(i in 0 until 6) {
                val tree=heightSized(prototype("Background"),"MissionForest_$i",unit*2.5f)
                tree.components[TransformComponent::class.java]!!.apply {
                    position=Vector3((-3.2f+i*1.28f)*unit,0f,-unit*1.8f)
                    eulerAngles=EulerAngles(0f,i*37f,0f)
                }
            }
        } else if(outdoor) {
            val forest=clone(prototype("Background"),"MissionForest")
            val factor=minOf(width*0.99f/4.92f,depth*0.42f/0.69f)
            forest.components[TransformComponent::class.java]!!.apply {scaleVector=Vector3(factor,factor,factor);position=Vector3(0f,0f,-0.14f*factor)}
        }
        if(outdoor && generatedModels.containsKey("Mountains")) {
            for(i in 0 until 3) heightSized(prototype("Mountains"),"MissionRuins_$i",unit*1.5f).components[TransformComponent::class.java]!!.position=Vector3((-3f+i*3f)*unit,0f,-unit*2.4f)
        } else if(outdoor) {
            val mountains=clone(prototype("Mountains"),"MissionMountains")
            val factor=minOf(width*0.99f/5.5f,depth*0.42f/0.67f)
            mountains.components[TransformComponent::class.java]!!.scaleVector=Vector3(factor,factor,factor)
        }
        if(!outdoor && generatedModels.containsKey("Industrial")) {
            for(i in 0 until 3) heightSized(prototype("Industrial"),"MissionIndustrial_$i",unit*2.2f).components[TransformComponent::class.java]!!.apply {
                position=Vector3((-3.1f+i*3.1f)*unit,0f,-unit*2.3f)
                eulerAngles=EulerAngles(0f,if(i%2==0)0f else 180f,0f)
            }
        }
    }

    private fun updateTerrainAppearance(theme: String) {
        if(lastTerrainTheme==theme)return
        val ground=when(theme) {
            "snow"->Color4(0.18f,0.25f,0.28f,1f)
            "furnace"->Color4(0.12f,0.05f,0.03f,1f)
            "alien"->Color4(0.09f,0.05f,0.14f,1f)
            "jungle","bridge","waterfall"->Color4(0.06f,0.11f,0.07f,1f)
            else->Color4(0.06f,0.09f,0.12f,1f)
        }
        val cap=when(theme) {
            "snow"->Color4(0.85f,0.93f,0.96f,1f)
            "furnace"->Color4(0.4f,0.15f,0.05f,1f)
            "alien"->Color4(0.25f,0.16f,0.36f,1f)
            "jungle","bridge","waterfall"->Color4(0.28f,0.43f,0.13f,1f)
            else->Color4(0.18f,0.28f,0.34f,1f)
        }
        for(part in direct(scope,"Terrain").getChildren()) {
            val color=if(part.getName().startsWith("Moss"))cap else ground
            part.components[ModelComponent::class.java]?.let { model ->
                for(i in 0 until model.materials.size) (model.materials[i] as? PhysicallyBasedMaterial)?.setBaseColor(color)
            }
        }
        lastTerrainTheme=theme
    }

    private fun rebuildMission(engine: GameEngine) {
        heroAnimation?.close();heroAnimation=null
        enemyAnimations.values.forEach {it.close()};enemyAnimations.clear()
        generatedModels["PlayerLive"]?.let {archetypes.addChild(it)}
        val old = active.getChildren().toSet()
        old.forEach { it.destroy() }
        visualEntities.removeAll(old)
        old.forEach {visibility.remove(it)}
        enemyModels.clear(); pickupModels.clear(); groundModels.clear(); platformModels.clear()
        friendlyPool.clear(); hostilePool.clear(); bulletModels.clear(); baseScales.clear()
        previousX = 0.5f
        buildMission(engine)
    }

    private fun prototype(name: String): Entity = generatedModels[name] ?: direct(scope,name)
    private fun direct(parent:Entity,name:String):Entity = requireNotNull(parent.getChildren().singleOrNull{it.getName()==name}) {"Missing or ambiguous Editor node ${parent.getName()}/$name"}
    private fun clone(proto:Entity,name:String,parent:Entity=active):Entity {
        val e=requireNotNull(proto.clone(Entity.CloneOptions(recursive=true,shouldShareMaterialInstance=true))) {"Editor archetype clone failed: $name"}
        e.setName(name);parent.addChild(e);setVisible(e,true);visualEntities+=e
        return e
    }
    private fun heightSized(proto:Entity,name:String,targetHeight:Float,parent:Entity=active):Entity {
        val b=proto.getVisualBounds(proto.getParent(),recursive=true)
        val f=targetHeight/(b.halfExtent.y*2f).coerceAtLeast(0.001f)
        val original=proto.components[TransformComponent::class.java]!!.scaleVector
        return clone(proto,name,parent).also {e->
            val s=Vector3(original.x*f,original.y*f,original.z*f)
            e.components[TransformComponent::class.java]!!.scaleVector=s;baseScales[e]=s
        }
    }
    private fun sized(proto:Entity,name:String,w:Float,h:Float,d:Float,parent:Entity=active):Entity {
        val b=proto.getVisualBounds(proto.getParent(),recursive=true)
        val original=proto.components[TransformComponent::class.java]!!.scaleVector
        return clone(proto,name,parent).also{e->e.components[TransformComponent::class.java]!!.scaleVector=
            Vector3(original.x*w/(b.halfExtent.x*2f).coerceAtLeast(0.001f),original.y*h/(b.halfExtent.y*2f).coerceAtLeast(0.001f),original.z*d/(b.halfExtent.z*2f).coerceAtLeast(0.001f))}
    }
    private fun setVisible(entity:Entity,visible:Boolean) {
        visibility.getOrPut(entity) {EntityTreeVisibility(entity)}.setVisible(visible)
    }
    private fun worldX(x:Float,camera:Float)=(x-camera-3.65f)*unit
    private fun visible(x:Float,camera:Float)=x>=camera-0.4f&&x<=camera+7.7f
    private fun place(e:Entity,x:Float,y:Float,z:Float=0f) {e.components[TransformComponent::class.java]!!.position=Vector3(x,y,z)}

    override fun update(engine:GameEngine) {
        if(closed)return
        val theme=if(engine.mode==GameMode.TITLE) "jungle" else engine.level.theme
        applyEnvironment(theme)
        updateTerrainAppearance(theme)
        if(missionRevision != engine.missionRevision) rebuildMission(engine)
        val isTitle=engine.mode==GameMode.TITLE
        referenceVisibility.setVisible(isTitle);active.enabled=!isTitle
        if(isTitle) {active.getChildren().forEach {setVisible(it,false)};return}
        staticMissionEntities.forEach {setVisible(it,true)}
        val cam=engine.cameraX
        setVisible(player,engine.invulnerable<=0f||(engine.elapsed*12f).toInt()%2==0)
        val base=baseScales[player]!!
        player.components[TransformComponent::class.java]!!.apply {
            position=Vector3(worldX(engine.playerX,cam),engine.playerY*unit,0f)
            scaleVector=Vector3(base.x,base.y*if(engine.crouching && heroAnimation==null)0.45f else 1f,base.z)
            eulerAngles=EulerAngles(0f,if(engine.facing<0f)180f else 0f,0f)
        }
        if(engine.elapsed!=lastAnimatedTime) {moving=abs(engine.playerX-previousX)>0.0001f;lastAnimatedTime=engine.elapsed}
        heroAnimation?.update(when {engine.prone->"prone";engine.crouching->"crouch";!engine.grounded->"jump";moving->"walk";else->"idle"},engine.mode!=GameMode.PLAYING)
        for((id,animation)in enemyAnimations)animation.update("walk",engine.mode!=GameMode.PLAYING || enemyModels[id]?.enabled!=true,0.25f*engine.level.walkFactor)
        val walk=if(engine.grounded&&abs(engine.playerX-previousX)>0.0001f)sin(engine.elapsed*18f)*18f else 0f
        leftLeg?.components?.get(TransformComponent::class.java)?.eulerAngles=EulerAngles(0f,0f,walk)
        rightLeg?.components?.get(TransformComponent::class.java)?.eulerAngles=EulerAngles(0f,0f,-walk)
        val angle=engine.aimAngle*180f/PI.toFloat()
        rifle?.components?.get(TransformComponent::class.java)?.eulerAngles=EulerAngles(0f,0f,if(engine.facing>0f)angle else 180f-angle)
        previousX=engine.playerX
        for(e in engine.enemies) enemyModels[e.id]?.let {model->
            setVisible(model,e.health>0&&visible(e.x,cam))
            if(model.enabled) {
                place(model,worldX(e.x,cam),e.y*unit)
                val role=when(e.type){2->"Guardian";1->"Turret";else->"Enemy"}
                if(generatedModels.containsKey(role)) model.components[TransformComponent::class.java]!!.eulerAngles=EulerAngles(0f,if((e.type==0 && e.walk<0f) || (e.type!=0 && e.x>engine.playerX))180f else 0f,0f)
                else if(e.type==0)model.components[TransformComponent::class.java]!!.eulerAngles=EulerAngles(0f,if(e.x<engine.playerX)180f else 0f,0f)
            }
        }
        for(p in engine.pickups)pickupModels[p.id]?.let{model->setVisible(model,!p.taken&&visible(p.x,cam));if(model.enabled)place(model,worldX(p.x,cam),(p.y+0.15f+sin(engine.elapsed*3f)*0.035f)*unit,0.03f*unit)}
        val ground=engine.platforms.filter{it.ground}
        for(i in ground.indices){val p=ground[i];val pair=groundModels[i];val x=worldX(p.x+p.width/2,cam)
            place(pair.first,x,(-0.65f/2-0.045f)*unit);place(pair.second,x,-0.045f/2*unit)
        }
        val elevated=engine.platforms.filter{!it.ground}
        for(i in elevated.indices){val p=elevated[i];val pair=platformModels[i];val x=worldX(p.x+p.width/2,cam)
            place(pair.first,x,(p.y-0.18f/2)*unit);place(pair.second,x,(p.y-0.18f)/2*unit,-0.65f*unit)
        }
        val live=HashSet<Int>()
        for(b in engine.bullets){val key=b.id*2+if(b.friendly)1 else 0;live.add(key)
            var sprite=bulletModels[key]
            if(sprite==null){val pool=if(b.friendly)friendlyPool else hostilePool
                if(pool.isEmpty())continue
                sprite=pool.removeFirst() to b.friendly;bulletModels[key]=sprite
            }
            setVisible(sprite.first,visible(b.x,cam))
            sprite.first.components[TransformComponent::class.java]!!.apply {position=Vector3(worldX(b.x,cam),b.y*unit,0.06f*unit);eulerAngles=EulerAngles(0f,0f,atan2(b.vy,b.vx)*180f/PI.toFloat())}
        }
        val it=bulletModels.iterator()
        while(it.hasNext()){val entry=it.next();if(entry.key !in live){val (e,friendly)=entry.value;setVisible(e,false);(if(friendly)friendlyPool else hostilePool).addLast(e);it.remove()}}
    }
    override fun close(){if(closed)return;closed=true;heroAnimation?.close();enemyAnimations.values.forEach{it.close()};root.destroy();bulletModels.clear();friendlyPool.clear();hostilePool.clear();visualEntities.clear();visibility.clear()}
}
