import org.w3c.dom.CanvasRenderingContext2D
import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.reflect.KClass

data class Circle(var radius: Double) : Component

class CirclesContext(
    dimContext: DimContext
) : DimContext(dimContext.width, dimContext.height, dimContext.scale)

open class DimContext(
    val width: Double,
    val height: Double,
    val scale: Double = 1.0
) : EmptyContext()


@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class MovingSystem(
    private val width: Double,
    private val height: Double
) : Component3System<Position, Velocity, Circle, EmptyContext>(Position::class, Velocity::class, Circle::class) {
    override fun doProcessEntity(position: Position, velocity: Velocity, circle: Circle) {
        val nextPosition = position.v.copy().add(velocity.v)
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
        if (collision && circle.radius > 5.0 && Random.nextDouble() < 0.1) {
            val splitK = 0.8
            val velK = 0.4
            circle.radius *= splitK
            val v1 = velocity.v.copy()
            velocity.v.scale(velK)
            world.createCircle(
                nextPosition,
                circle.radius * (1 - splitK),
                Vector.one()
                    .scale(sqrt(v1.lengthSq() * (1 - splitK * velK * velK) / (1 - splitK)))
                    .rot(Random.nextDouble(PI * 2))
            )
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
    override fun doProcessEntity(position: Position, circle: Circle) {
        position.r.rot(1.0 / circle.radius)
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

    override fun doProcessEntity(position: Position, circle: Circle) {
        ctx.scale(world.globals.scale, world.globals.scale)
        ctx.beginPath()
        ctx.ellipse(position.v.x, position.v.y, circle.radius, circle.radius, 0.0, 0.0, 2 * PI)
        ctx.closePath()
        ctx.stroke()
        ctx.resetTransform()
    }
}

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class DebugRenderSystem(private val ctx: CanvasRenderingContext2D) :
    Component3System<Position, Circle, Velocity, DimContext>(Position::class, Circle::class, Velocity::class) {
    override fun doProcessEntity(position: Position, circle: Circle, velocity: Velocity) {
        ctx.scale(world.globals.scale, world.globals.scale)
        val originalStroke = ctx.strokeStyle
        ctx.strokeStyle = "#ff2020"
        ctx.translate(position.v.x, position.v.y)
        ctx.beginPath()
        ctx.moveTo(0.0, 0.0)
        velocity.v.copy().scale(10.0).let { ctx.lineTo(it.x, it.y) }
        ctx.stroke()
        ctx.beginPath()
        ctx.moveTo(0.0, 0.0)
        position.r.copy().normalize().scale(circle.radius).let { ctx.lineTo(it.x, it.y) }
        ctx.strokeStyle = "#20ff20"
        ctx.stroke()
        ctx.resetTransform()
        ctx.strokeStyle = originalStroke
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

fun World<EmptyContext>.createCircle(position: Vector, radius: Double, velocity: Vector) {
    val e = createEntity()
    val pos = addComponent(e, Position::class)
    val circle = addComponent(e, Circle::class)
    val vel = addComponent(e, Velocity::class)
    pos.v.set(position)
    circle.radius = radius
    vel.v.set(velocity)
}