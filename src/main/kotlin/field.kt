import org.w3c.dom.CanvasLineCap
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.ROUND
import kotlin.math.*
import kotlin.math.sin
import kotlin.random.Random
import kotlin.reflect.KClass

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class FieldSystem(
) : Component1System<Position, EmptyContext>(Position::class) {
    override fun doProcessEntity(entity: Int, position: Position) {
        val player = world.tags["player"]
        if (player != null) {
            val playerPos = world.component(player, Position::class)!!
            val dv = playerPos.v - position.v
            position.r.set(dv.normalized() * sin(exp(dv.length() / 100)) * 10.0)
        }
    }
}

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class FieldRenderSystem(
    private val ctx: CanvasRenderingContext2D
) : Component1System<Position, DimContext>(Position::class) {
    override fun before() {
        ctx.fillStyle = "#ffffff"
        ctx.fillRect(0.0, 0.0, world.globals.width, world.globals.height)
    }

    override fun doProcessEntity(entity: Int, position: Position) {
        ctx.scale(world.globals.scale, world.globals.scale)
        ctx.translate(position.v.x, position.v.y)
        drawRotation(position)
        ctx.resetTransform()
    }

    private fun drawRotation(position: Position) {
        ctx.beginPath()
        ctx.moveTo(0.0, 0.0)
        ctx.lineCap = CanvasLineCap.ROUND
        (position.r).let {
            ctx.lineTo(it.x, it.y)
            ctx.ellipse(it.x, it.y, 1.0, 1.0, 0.0, 0.0, 2* PI)
        }
        ctx.strokeStyle = "#20ff20"
        ctx.stroke()
    }
}

class FieldInputSystem(val mousePosProvider: () -> Vector) : AbstractSystem<EmptyContext>() {
    override fun doProcessEntity(entity: Int) {
    }

    override fun after() {
        world.tags["player"]?.let { e ->
            world.component(e, Position::class)?.let { position ->
                val mp = mousePosProvider()
                position.v.set(mp)
            }
        }
    }
}

fun fieldWorld(context: DimContext) = World(object : RegisteredComponents {
    override val components: Map<KClass<out Component>, () -> Component>
        get() = mapOf(
            Position::class to { Position(Vector.zero()) }
        )
}, context)

fun World<DimContext>.createVector(position: Vector): Int {
    val e = createEntity()
    val pos = addComponent(e, Position::class)
    pos.v.set(position)
    return e
}