import org.w3c.dom.CanvasRenderingContext2D
import kotlin.math.PI
import kotlin.reflect.KClass

data class Circle(var radius: Double) : Component


@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class MovingSystem(
    private val width: Double,
    private val height: Double
) : Component3System<Position, Velocity, Circle>(Position::class, Velocity::class, Circle::class) {
    override fun doProcessEntity(position: Position, velocity: Velocity, circle: Circle) {
        if ((position.v.x - circle.radius <= 0 && velocity.v.x < 0) ||
            (position.v.x + circle.radius >= width && velocity.v.x > 0)
        ) {
            velocity.v.x *= -1
            if (position.v.x - circle.radius < 0) position.v.x = circle.radius
            else position.v.x = width - circle.radius
        }
        if ((position.v.y - circle.radius <= 0 && velocity.v.y < 0) ||
            (position.v.y + circle.radius >= height && velocity.v.y > 0)
        ) {
            velocity.v.y *= -1
            if (position.v.y - circle.radius < 0) position.v.y = circle.radius
            else position.v.y = height - circle.radius
        }
        val vel = velocity.v.copy()
        world.delay { position.v.add(vel) }
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