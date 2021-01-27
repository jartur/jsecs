data class Vector(var x: Double, var y: Double) {
    companion object {
        fun zero() = Vector(0.0, 0.0)
    }

    fun add(v: Vector) {
        x += v.x
        y += v.y
    }
}

data class Position(val v: Vector) : Component
data class Velocity(val v: Vector) : Component