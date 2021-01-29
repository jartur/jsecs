import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vector(var x: Double, var y: Double) {
    companion object {
        fun zero() = Vector(0.0, 0.0)
        fun one() = Vector(1.0, 1.0)
    }

    operator fun plusAssign(v: Vector) {
        x += v.x
        y += v.y
    }

    operator fun plus(v: Vector) = Vector(x + v.x, y + v.y)

    fun set(vector: Vector): Vector {
        x = vector.x
        y = vector.y
        return this
    }

    operator fun timesAssign(s: Double) {
        x *= s
        y *= s
    }

    operator fun times(s: Double): Vector = Vector(x * s, y * s)

    fun rot(r: Double): Vector {
        x = x * cos(r) - y * sin(r)
        y = x * sin(r) + y * cos(r)
        return this
    }

    fun lengthSq(): Double = x * x + y * y

    fun length(): Double = sqrt(lengthSq())

    fun normalize() {
        length().let {
            x /= it
            y /= it
        }
    }

    fun normalized(): Vector = copy().also { it.normalize() }

    operator fun minusAssign(v: Vector) {
        x -= v.x
        y -= v.y
    }

    operator fun minus(v: Vector): Vector = Vector(x - v.x, y - v.y)
    operator fun compareTo(d: Double): Int = lengthSq().compareTo(d * d)
    operator fun div(d: Double): Vector = Vector(x / d, y / d)
    operator fun divAssign(d: Double) {
        x /= d
        y /= d
    }
}

data class Position(val v: Vector, val r: Vector = Vector(1.0, 0.0)) : Component
data class Velocity(val v: Vector) : Component

open class DimContext(
    val width: Double,
    val height: Double,
    val scale: Double = 1.0
) : EmptyContext()