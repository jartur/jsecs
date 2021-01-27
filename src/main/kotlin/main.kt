import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import kotlin.math.PI
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

val cvs = document.getElementById("cvs") as HTMLCanvasElement
val ctx = cvs.getContext("2d")!! as CanvasRenderingContext2D

val world = circlesWorld

fun main() {
    val scale = 1
    val width = 700
    val height = 600
    cvs.width = width * scale
    cvs.height = height * scale
    world.registerSystem(MovingSystem(width.toDouble(), height.toDouble()))
    world.registerSystem(CircleRenderSystem(cvs.width, cvs.height, ctx, scale))
    for (i in (0..4000)) {
        val e = world.createEntity()
        val pos = world.addComponent(e, Position::class)
        val circle = world.addComponent(e, Circle::class)
        val vel = world.addComponent(e, Velocity::class)
        pos.v.x = Random.nextDouble(width.toDouble())
        pos.v.y = Random.nextDouble(height.toDouble())
        circle.radius = Random.nextDouble(40.0)
        vel.v.x = Random.nextDouble(5.0)
        vel.v.y = Random.nextDouble(5.0)
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