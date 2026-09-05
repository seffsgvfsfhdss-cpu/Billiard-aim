package com.example.billiardaim

import kotlin.math.sqrt

data class Vector2D(var x: Float, var y: Float) {
    fun add(v: Vector2D) = Vector2D(x + v.x, y + v.y)
    fun sub(v: Vector2D) = Vector2D(x - v.x, y - v.y)
    fun scale(s: Float) = Vector2D(x * s, y * s)
    fun length(): Float = sqrt(x * x + y * y)
    fun normalize(): Vector2D {
        val len = length()
        return if (len > 0) Vector2D(x / len, y / len) else Vector2D(0f, 0f)
    }
    fun distance(v: Vector2D): Float = sub(v).length()
}
