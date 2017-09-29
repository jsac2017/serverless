package main.plots

import utils.Range

abstract class PointGenerator<U, V> {
    abstract protected fun initialParameters(value: U): V

    private val props: MutableList<Pair<() -> Range?, (Double, U, V) -> V>> = mutableListOf()

    protected fun addProp(range: () -> Range?, transform: (Double, U, V) -> V) {
        props.add(range to transform)
    }

    private fun <R> forEachLocal(propIdx: Int, u: U, v: V, value: Double?, f: (V, Double?) -> R): List<R> {
        if (propIdx == props.size) {
            return listOf(f(v, value))
        }

        val (getRange, transformParam) = props[propIdx]
        val range = getRange()

        if (range != null && !range.isPoint() && value != null) {
            throw IllegalStateException("Only one parameter can vary!")
        }

        return range?.flatMap {
            forEachLocal(propIdx + 1, u, transformParam(it, u, v),
                    if (!range.isPoint()) it else value, f)
        } ?: forEachLocal(propIdx + 1, u, v, value, f)
    }

    private fun <R> forEachPoint(u: U, value: Double?, f: (V, Double?) -> R): List<R> =
        forEachLocal(0, u, initialParameters(u), value, f)

    fun <R> then(u: U, f: (V) -> (Double?) -> List<R>): (Double?) -> List<R> =
            { x1 -> forEachPoint(u, x1, { v, x2 -> f(v)(x2)}).flatten() }

    fun <R> then(u: U, f: (V, Double?) -> R): (Double?) -> List<R> =
            { x -> forEachPoint(u, x, f) }

    fun <R> start(u: U, f: (V, Double?) -> R): List<R> =
            forEachPoint(u, null, { v, x -> f(v, x)})
}

fun <R, U> PointGenerator<Unit, U>.startUnit(f: (U) -> (Double?) -> List<R>): List<R> =
        start(Unit, { v, x -> f(v)(x)}).flatten()

fun <R, U> PointGenerator<Unit, U>.startUnit(f: (U, Double?) -> R): List<R> =
        start(Unit,  { v, x -> f(v, x)})


