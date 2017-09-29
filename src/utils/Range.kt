package utils

import java.text.DecimalFormat

class Range private constructor(
        val start: Double = 0.0,
        val end: Double = 0.0,
        val numSegments: Int = 0,
        val exponential: Boolean = false) : Iterable<Double> {
    companion object {
        fun linear(start: Double, end: Double, numSegments: Int): Range {
            if (start >= end) {
                throw IllegalArgumentException("In linear range start must be less than end")
            }
            if (numSegments <= 0) {
                throw IllegalStateException("In linear range number of segments must be at least one!")
            }
            return Range(start, end, numSegments, false)
        }

        fun exponential(start: Double, end:Double, numSegments: Int): Range {
            if (start <= 0) {
                throw IllegalArgumentException("In exponential range start must be greater than zero")
            }
            if (start >= end) {
                throw IllegalArgumentException("In exponential range start must be less than end")
            }
            if (numSegments <= 0) {
                throw IllegalStateException("In exponential range number of segments must be at least one!")
            }
            return Range(start, end, numSegments, true)
        }

        fun point(value: Double): Range {
            return Range(value, value, 0, false)
        }
    }

    fun format(format: DecimalFormat, sep: String): String = if (numSegments == 0) format.format(start) else
       listOf(start, end, numSegments).map { format.format(it) }.joinToString(sep) +
               optional(exponential, { sep + "exp" })

    override fun toString(): String = format(DecimalFormat("0.###"), ":")

    fun isPoint() = numSegments == 0

    override fun iterator(): Iterator<Double> =
        (0..numSegments).map {
            if (numSegments == 0) {
                start
            } else if (exponential) {
                start * Math.pow(Math.pow(end / start, 1.0 / numSegments), it.toDouble())
            } else {
                start + it * (end - start) / numSegments
            }
        }.iterator()
}