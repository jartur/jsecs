import org.w3c.dom.CanvasRenderingContext2D
import kotlin.math.sin
import kotlin.reflect.KClass

data class Cell(
    var alive: Double,
    var x: Int = 0,
    var y: Int = 0,
    var neighbours: MutableList<Int> = mutableListOf()
) : Component

class CellularAutomatonSystem : ComponentSystem(setOf(Cell::class)) {
    override fun doProcessEntity(entity: Int) {
        world.component(entity, Cell::class)!!.let { cell ->
            val updated = cellAliveNextTurn(cell)
            world.delay { cell.alive = updated }
        }
    }

    private fun cellAliveNextTurn(cell: Cell): Double {
        val aliveNeighbours = cell.neighbours.sumByDouble { id ->
            world.component(id, Cell::class)!!.alive
        }
        return sin(aliveNeighbours)
    }
}

class AutomatonRenderSystem(
    private val width: Int,
    private val height: Int,
    private val ctx: CanvasRenderingContext2D,
    private val scale: Int = 1
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

val automatonWorld = World(object : RegisteredComponents {
    override val components: Map<KClass<out Component>, () -> Component>
        get() = mapOf(
            Cell::class to { Cell(0.0) }
        )
})