import org.w3c.dom.CanvasRenderingContext2D
import kotlin.math.*
import kotlin.math.sin
import kotlin.random.Random
import kotlin.reflect.KClass

data class Circle(var radius: Double) : Component

class CirclesContext(
    dimContext: DimContext
) : DimContext(dimContext.width, dimContext.height, dimContext.scale)

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class MovingSystem(
    private val width: Double,
    private val height: Double
) : Component3System<Position, Velocity, Circle, EmptyContext>(Position::class, Velocity::class, Circle::class) {
    override fun doProcessEntity(entity: Int, position: Position, velocity: Velocity, circle: Circle) {
        val player = world.tags["player"]
        val isPlayer = player == entity
        val nextPosition = position.v + velocity.v
        var collision = false
        if ((nextPosition.x - circle.radius <= 0 && velocity.v.x < 0) ||
            (nextPosition.x + circle.radius >= width && velocity.v.x > 0)
        ) {
            if (nextPosition.x - circle.radius < 0) nextPosition.x = circle.radius + 1
            else nextPosition.x = width - circle.radius - 1
            velocity.v.x *= -0.9
            collision = true
        }
        if ((nextPosition.y - circle.radius <= 0 && velocity.v.y < 0) ||
            (nextPosition.y + circle.radius >= height && velocity.v.y > 0)
        ) {
            if (nextPosition.y - circle.radius < 0) nextPosition.y = circle.radius + 1
            else nextPosition.y = height - circle.radius - 1
            velocity.v.y *= -0.9
            collision = true
        }
        if (collision && !isPlayer && circle.radius > 5.0 && Random.nextDouble() < 0.1) {
            val splitK = 0.8
            val velK = 0.4
            circle.radius *= splitK
            val v1 = velocity.v.copy()
            velocity.v *= velK
            world.createCircle(
                nextPosition,
                circle.radius * (1 - splitK),
                (Vector.one() * (sqrt(v1.lengthSq() * (1 - splitK * velK * velK) / (1 - splitK))))
                    .rot(Random.nextDouble(PI * 2))
            )
        }
        velocity.v *= 0.999
        velocity.v += position.r.normalized() * 0.01
        player?.let {
            world.component(it, Position::class)?.let { playerPos ->
                val runV = position.v - playerPos.v
                if (runV > 0.0) {
                    val ds = runV.length() - world.component(it, Circle::class)!!.radius - circle.radius
                    runV.normalize()
                    runV *= 2 * cos(ds/50.0)
                    position.r += runV
                    position.r.normalize()
                }
            }
        }
        // Delay the position update so that other circles see a static picture of the world
        // during a frame.
        world.delay {
            position.v.set(nextPosition)
        }
    }
}

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class RotatingSystem : Component2System<Position, Circle, EmptyContext>(Position::class, Circle::class) {
    override fun doProcessEntity(entity: Int, position: Position, circle: Circle) {
       // position.r.rot(0.1 / circle.radius)
    }
}

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class CircleRenderSystem(
    private val ctx: CanvasRenderingContext2D
) : Component2System<Position, Circle, DimContext>(Position::class, Circle::class) {
    override fun before() {
        ctx.fillStyle = "#ffffff"
        ctx.fillRect(0.0, 0.0, world.globals.width, world.globals.height)
    }

    override fun doProcessEntity(entity: Int, position: Position, circle: Circle) {
        ctx.scale(world.globals.scale, world.globals.scale)
        ctx.beginPath()
        ctx.ellipse(position.v.x, position.v.y, circle.radius, circle.radius, 0.0, 0.0, 2 * PI)
        ctx.closePath()
        ctx.stroke()
        ctx.resetTransform()
    }
}

class InputSystem(val mousePosProvider: () -> Vector) : AbstractSystem<EmptyContext>() {
    override fun doProcessEntity(entity: Int) {

    }

    override fun after() {
        world.tags["player"]?.let { e ->
            world.component(e, Velocity::class)?.let { velocity ->
                world.component(e, Position::class)?.let { position ->
                    val mp = mousePosProvider()
                    val dv = mp - position.v
                    if (dv > 1.0) {
                        velocity.v.set(dv / 10.0)
                    } else {
                        velocity.v.set(Vector.zero())
                    }
                }
            }
        }
    }
}

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class DebugRenderSystem(private val ctx: CanvasRenderingContext2D) :
    Component3System<Position, Circle, Velocity, DimContext>(Position::class, Circle::class, Velocity::class) {
    override fun doProcessEntity(entity: Int, position: Position, circle: Circle, velocity: Velocity) {
        ctx.scale(world.globals.scale, world.globals.scale)
        val originalStroke = ctx.strokeStyle
        ctx.strokeStyle = "#ff2020"
        ctx.translate(position.v.x, position.v.y)
        drawVelocity(velocity)
        drawRotation(position, circle)
        ctx.resetTransform()
        ctx.strokeStyle = originalStroke
    }

    override fun after() {
        ctx.strokeText("ENTITIES: ${world.entityCount}", 10.0, 10.0)
    }

    private fun drawRotation(position: Position, circle: Circle) {
        ctx.beginPath()
        ctx.moveTo(0.0, 0.0)
        (position.r.normalized() * circle.radius).let { ctx.lineTo(it.x, it.y) }
        ctx.strokeStyle = "#20ff20"
        ctx.stroke()
    }

    private fun drawVelocity(velocity: Velocity) {
        ctx.beginPath()
        ctx.moveTo(0.0, 0.0)
        (velocity.v * 10.0).let { ctx.lineTo(it.x, it.y) }
        ctx.stroke()
    }
}

fun circlesWorld(context: CirclesContext) = World(object : RegisteredComponents {
    override val components: Map<KClass<out Component>, () -> Component>
        get() = mapOf(
            Position::class to { Position(Vector.zero()) },
            Velocity::class to { Velocity(Vector.zero()) },
            Circle::class to { Circle(0.0) }
        )
}, context)

fun World<EmptyContext>.createCircle(position: Vector, radius: Double, velocity: Vector): Int {
    val e = createEntity()
    val pos = addComponent(e, Position::class)
    val circle = addComponent(e, Circle::class)
    val vel = addComponent(e, Velocity::class)
    pos.v.set(position)
    pos.r.set(velocity.copy())
    circle.radius = radius
    vel.v.set(velocity)
    return e
}