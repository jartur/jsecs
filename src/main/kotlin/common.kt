data class Vector(var x: Double, var y: Double) {
    companion object {
        fun zero() = Vector(0.0, 0.0)
    }

    fun add(v: Vector): Vector {
        x += v.x
        y += v.y
        return this
    }

    fun set(vector: Vector): Vector {
        x = vector.x
        y = vector.y
        return this
    }
}

data class Position(val v: Vector) : Component
data class Velocity(val v: Vector) : Component