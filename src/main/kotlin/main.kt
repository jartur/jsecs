import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

val cvs = document.getElementById("cvs") as HTMLCanvasElement
val ctx = cvs.getContext("2d")!! as CanvasRenderingContext2D
val width = 700
val height = 600

val world = circlesWorld(CirclesContext(DimContext(width.toDouble(), height.toDouble())))

fun main() {
    val scale = 1
    cvs.width = width * scale
    cvs.height = height * scale
    world.registerSystem(MovingSystem(width.toDouble(), height.toDouble()))
    world.registerSystem(RotatingSystem())
    world.registerSystem(CircleRenderSystem(ctx))
    world.registerSystem(DebugRenderSystem(ctx))
    for (i in (0..40)) {
        world.createCircle(
            position = Vector(
                width.toDouble() / 2,
                height.toDouble() / 2
            ),
            radius = Random.nextDouble(1.0, 40.0),
            velocity = Vector(
                Random.nextDouble(-5.0, 5.0),
                Random.nextDouble(-5.0, 5.0)
            )
        )
    }
}

@Suppress("unused")
@ExperimentalTime
fun run() {
    window.setInterval({
        val t = measureTime {
            world.tick()
        }
        console.log(t.inMilliseconds)
    }, 13)
}