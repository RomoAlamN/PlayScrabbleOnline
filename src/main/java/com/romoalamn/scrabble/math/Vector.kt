package com.romoalamn.scrabble.math

import java.lang.ArithmeticException
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


class Vector(var x: Int, var y: Int)  {

}
class VectorRange(
    private val first: Vector,
    private val last: Vector
) :  Iterable<Vector> {
    override fun iterator(): Iterator<Vector> {
        return object : Iterator<Vector>{
            val sizeX = (last.x - first.x)+1 // counting initial value
            val sizeY = (last.y - first.y) + 1 //counting initial value
            private val finalElement = sizeX * sizeY - 1 // start at zero
            private var hasNext = finalElement != 0
            var current = 0
            override fun hasNext(): Boolean = hasNext
            override fun next(): Vector {
                val value = current
                if(value == finalElement){
                    if(!hasNext) throw NoSuchElementException()
                    hasNext = false
                }else{
                    current++
                }
                val x = value % sizeY
                return Vector(x,(value-x)/sizeY)
            }

        }
    }

}


operator fun Vector.rangeTo(other: Vector): VectorRange {
    return VectorRange(this, other)
}