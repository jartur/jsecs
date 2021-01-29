import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.MouseEvent
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

val cvs = document.getElementById("cvs") as HTMLCanvasElement
val ctx = cvs.getContext("2d")!! as CanvasRenderingContext2D
const val width = 700
const val height = 600

val circlesWorld = circlesWorld(CirclesContext(DimContext(width.toDouble(), height.toDouble())))
val fieldWorld = fieldWorld(DimContext(width.toDouble(), height.toDouble()))

fun getMousePos(canvas: HTMLCanvasElement, e: MouseEvent): Vector {
    val r = canvas.getBoundingClientRect()
    return Vector(
        e.clientX.toDouble() - r.left,
        e.clientY.toDouble() - r.top
    )
}

var lastMousePos: Vector = Vector((width / 2).toDouble(), (height / 2).toDouble())

fun main() {
    val scale = 1
    cvs.width = width * scale
    cvs.height = height * scale
    initFieldWorld()
}

private fun initFieldWorld() {
    val fieldInputSystem = FieldInputSystem()
    cvs.onmousedown = {
        fieldInputSystem.clicked = true
        false
    }
    cvs.onmousemove = { e ->
        fieldInputSystem.mousePos = getMousePos(cvs, e)
        false
    }
    fieldWorld.registerSystem(fieldInputSystem)
    fieldWorld.registerSystem(FieldSystem())
    fieldWorld.registerSystem(FieldRenderSystem(ctx))
    val vpd = 30
    for (x in (0..vpd)) {
        for (y in (0..vpd)) {
            fieldWorld.createVector(Vector((width / vpd * x).toDouble(), (height / vpd * y).toDouble()))
        }
    }
    fieldWorld.createVector(Vector.zero()).also { fieldWorld.tag("player", it) }
}

@Suppress("unused")
private fun initCirclesWorld() {
    circlesWorld.registerSystem(MovingSystem(width.toDouble(), height.toDouble()))
    circlesWorld.registerSystem(RotatingSystem())
    circlesWorld.registerSystem(CircleRenderSystem(ctx))
    circlesWorld.registerSystem(DebugRenderSystem(ctx))
    circlesWorld.registerSystem(InputSystem { lastMousePos })
    for (i in (0..40)) {
        circlesWorld.createCircle(
            position = Vector(
                width.toDouble() / 8 * (i % 8),
                height.toDouble() / 5 * (i / 5)
            ),
            radius = Random.nextDouble(1.0, 40.0),
            velocity = Vector(
                Random.nextDouble(-1.0, 1.0),
                Random.nextDouble(-1.0, 1.0)
            )
        )
    }
    val player = circlesWorld.createCircle(
        position = lastMousePos,
        radius = 60.0,
        velocity = Vector.zero()
    )
    circlesWorld.tag("player", player)
}

@Suppress("unused")
@ExperimentalTime
fun run() {
    window.setInterval({
        val t = measureTime {
            fieldWorld.tick()
        }
        console.log(t.inMilliseconds)
    }, 20)
}