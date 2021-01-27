import kotlin.math.cos
import kotlin.math.sin

data class Vector(var x: Double, var y: Double) {
    companion object {
        fun zero() = Vector(0.0, 0.0)
        fun one() = Vector(1.0, 1.0)
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

    fun scale(s: Double): Vector {
        x *= s
        y *= s
        return this
    }

    fun rot(r: Double): Vector {
        x *= sin(r)
        y *= cos(r)
        return this
    }

    fun lengthSq(): Double = x*x + y*y
}

data class Position(val v: Vector) : Component
data class Velocity(val v: Vector) : Component