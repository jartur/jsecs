import org.w3c.dom.CanvasRenderingContext2D
import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.reflect.KClass

data class Circle(var radius: Double) : Component


@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class MovingSystem(
    private val width: Double,
    private val height: Double
) : Component3System<Position, Velocity, Circle>(Position::class, Velocity::class, Circle::class) {
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
class CircleRenderSystem(
    private val width: Int,
    private val height: Int,
    private val ctx: CanvasRenderingContext2D,
    private val scale: Int = 1
) : Component2System<Position, Circle>(Position::class, Circle::class) {

    override fun before() {
        ctx.fillStyle = "#ffffff"
        ctx.fillRect(0.0, 0.0, width.toDouble(), height.toDouble())
    }

    override fun doProcessEntity(position: Position, circle: Circle) {
        ctx.scale(scale.toDouble(), scale.toDouble())
        ctx.beginPath()
        ctx.ellipse(position.v.x, position.v.y, circle.radius, circle.radius, 0.0, 0.0, 2 * PI)
        ctx.closePath()
        ctx.stroke()
        ctx.resetTransform()
    }
}

val circlesWorld = World(object : RegisteredComponents {
    override val components: Map<KClass<out Component>, () -> Component>
        get() = mapOf(
            Position::class to { Position(Vector.zero()) },
            Velocity::class to { Velocity(Vector.zero()) },
            Circle::class to { Circle(0.0) }
        )
})

fun World.createCircle(position: Vector, radius: Double, velocity: Vector) {
    val e = createEntity()
    val pos = addComponent(e, Position::class)
    val circle = addComponent(e, Circle::class)
    val vel = addComponent(e, Velocity::class)
    pos.v.set(position)
    circle.radius = radius
    vel.v.set(velocity)
}