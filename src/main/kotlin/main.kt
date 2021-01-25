import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

val cvs = document.getElementById("cvs") as HTMLCanvasElement
val ctx = cvs.getContext("2d")!! as CanvasRenderingContext2D

interface Component

data class Cell(
    var alive: Double,
    var x: Int = 0,
    var y: Int = 0,
    var neighbours: MutableList<Int> = mutableListOf()
) : Component

data class NextState(var alive: Double) : Component

object RegisteredComponents {
    val components: Map<KClass<out Component>, () -> Component> = mapOf(
        Cell::class to { Cell(0.0) },
        NextState::class to { NextState(0.0) }
    )
}

interface System {
    var enabled: Boolean
    fun processEntity(entity: Int)
    fun init(w: World)
    fun before() {}
    fun after() {}
}

abstract class AbstractSystem : System {
    override var enabled: Boolean = true
    lateinit var world: World

    override fun init(w: World) {
        world = w
    }

    open fun shouldRun(entity: Int): Boolean {
        if (!enabled) return false
        return true
    }

    abstract fun doProcessEntity(entity: Int)

    override fun processEntity(entity: Int) {
        if (shouldRun(entity)) {
            doProcessEntity(entity)
        }
    }
}

abstract class ComponentSystem(private val requiredComponents: Set<KClass<out Component>>) : AbstractSystem() {
    override fun shouldRun(entity: Int): Boolean {
        return super.shouldRun(entity) && requiredComponents.all { world.component(entity, it) != null }
    }
}

typealias Ents = MutableMap<KClass<out Component>, MutableMap<Int, Component?>>

interface EndFrameOp {
    fun run(entities: MutableList<Int>, components: Ents)
}

data class CreateEntity(val id: Int, var cs: MutableMap<KClass<out Component>, Component>) : EndFrameOp {
    override fun run(entities: MutableList<Int>, components: Ents) {
        entities.add(id)
        cs.forEach { c ->
            components[c.key]!![id] = c.value
        }
    }
}

class World {
    var components: Ents = mutableMapOf()
    var systems = mutableListOf<System>()
    var entities: MutableList<Int> = mutableListOf()
    private var freeEntities = mutableListOf<Int>()
    private var pendingOps = mutableMapOf<Int, EndFrameOp>()
    private var maxId = 0

    fun registerSystem(system: System) {
        system.init(this)
        systems.add(system)
    }

    fun tick() {
        systems.forEach { system ->
            if (system.enabled) {
                system.before()
                entities.forEach { i ->
                    system.processEntity(i)
                }
                system.after()
            }
        }
        pendingOps.values.forEach { op ->
            op.run(entities, components)
        }
        pendingOps.clear()
    }

    fun createEntity(): Int {
        val id = if (freeEntities.size > 0) freeEntities.removeAt(0) else maxId++
        pendingOps[id] = CreateEntity(id, mutableMapOf())
        return id
    }

    fun <T : Component> addComponent(e: Int, c: KClass<T>): T {
        if (!components.containsKey(c)) {
            components.put(c, mutableMapOf())
        }
        val component = RegisteredComponents.components[c]!!().unsafeCast<T>()
        if (pendingOps.containsKey(e)) {
            when (val op = pendingOps[e]) {
                is CreateEntity -> op.cs[c] = component
                else -> console.log("Operation not supported: $op")
            }
        } else {
            components[c]!![e] = component
        }
        return component
    }

    fun <T : Component> deleteComponent(e: Int, c: KClass<T>) {
        if (!components.containsKey(c)) {
            throw IllegalStateException("Component type is wrong $c")
        }
        if (pendingOps.containsKey(e)) {
            when (val op = pendingOps[e]) {
                is CreateEntity -> op.cs.remove(c)
                else -> console.log("Operation not supported: $op")
            }
        } else {
            components[c]!!.remove(e)
        }
    }

    fun <T : Component> component(e: Int, c: KClass<T>): T? = components[c]?.get(e)?.unsafeCast<T>()
}

class CellularAutomatonSystem : ComponentSystem(setOf(Cell::class)) {
    override fun doProcessEntity(entity: Int) {
        world.component(entity, Cell::class)!!.let { cell ->
            world.addComponent(entity, NextState::class).apply { alive = cellAliveNextTurn(cell) }
        }
    }

    private fun cellAliveNextTurn(cell: Cell): Double {
        val aliveNeighbours = cell.neighbours.sumByDouble { id ->
            world.component(id, Cell::class)!!.alive
        }
        return when {
            aliveNeighbours < 2 -> cell.alive * 0.9
            aliveNeighbours < 4 -> cell.alive * 1.1
            else -> cell.alive * 0.7
        }
    }
}

class NextStateApplySystem : ComponentSystem(setOf(NextState::class, Cell::class)) {
    override fun doProcessEntity(entity: Int) {
        world.component(entity, NextState::class)!!.let { nextState ->
            world.component(entity, Cell::class)!!.let { cell ->
                cell.alive = nextState.alive
            }
            world.deleteComponent(entity, NextState::class)
        }
    }
}

class RenderSystem(
    val width: Int,
    val height: Int,
    val ctx: CanvasRenderingContext2D,
    val scale: Int = 1
) : AbstractSystem() {

    override fun before() {
        ctx.fillStyle = "#ffffff"
        ctx.fillRect(0.0, 0.0, width.toDouble(), height.toDouble())
    }

    override fun doProcessEntity(entity: Int) {
        val cell = world.component(entity, Cell::class) ?: return
        ctx.fillStyle = "rgba(0, 0, 0, ${cell.alive.coerceIn(0.0, 1.0)})"
        ctx.scale(scale.toDouble(), scale.toDouble())
        ctx.fillRect(cell.x.toDouble(), cell.y.toDouble(), 1.0, 1.0)
        ctx.resetTransform()
    }
}

val world = World()

fun main() {
    val scale = 2
    val width = 200
    val height = 200
    cvs.width = width * scale
    cvs.height = height * scale
    world.registerSystem(CellularAutomatonSystem())
    world.registerSystem(NextStateApplySystem())
    world.registerSystem(RenderSystem(cvs.width, cvs.height, ctx, scale))
    val matrix = Array(width) { IntArray(height) }
    for (i in (0 until (width * height))) {
        val e = world.createEntity()
        val cell = world.addComponent(e, Cell::class)
        cell.alive = Random.nextDouble()
        val x = i % width
        val y = i / width
        cell.x = x
        cell.y = y
        matrix[x][y] = e
        for (dx in (-1..1)) {
            for (dy in (-1..1)) {
                if (!(x == 0 && y == 0) &&
                    x + dx > 0 && x + dx < width - 1 &&
                    y + dy > 0 && y + dy < height - 1
                ) {
                    cell.neighbours.add(matrix[x + dx][y + dy])
                }
            }
        }
    }
}

@ExperimentalTime
fun run() {
    window.setInterval({
        val t = measureTime {
            world.tick()
        }
        console.log(t.inMilliseconds)
    }, 100)
}