package utils

import java.util.TreeSet

class SumPQ<T>(comparator: Comparator<in T>, private val valueFun: (T) -> Double): Iterable<T> {
    val size get() = queue.size
    var sum = 0.0
        private set

    private val queue = TreeSet<T>(comparator)

    override fun iterator(): Iterator<T> = queue.iterator()

    fun add(element: T) {
        if (queue.add(element)) {
            sum += valueFun(element)
        }
    }

    fun pollFirst(): T? {
        val result = queue.pollFirst() ?: return null
        sum -= valueFun(result)
        return result
    }

    fun pollLast(): T? {
        val result = queue.pollLast() ?: return null
        sum -= valueFun(result)
        return result
    }

    fun clear() {
        queue.clear()
        sum = 0.0
    }
}

