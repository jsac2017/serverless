package model

import java.util.*

class ResourceManager(private val maxCapacity: Int, private val allocationTime: Int) {
    var numResourcesReady: Int = 0
        private set
    val numResourcesPending get() = pendingResources.size
    private val pendingResources = ArrayDeque<Resource>()

    fun tick() {
        pendingResources.forEach { it.tick() }
        checkNewResourcesReady()
    }

    fun requestCapacity(desiredCapacity: Int): Int {
        val delta = desiredCapacity - (numResourcesPending + numResourcesReady)
        if (delta < 0) {
            deallocate(-delta)
        } else {
            allocate(delta)
        }
        assert(desiredCapacity == numResourcesPending + numResourcesReady)
        return delta
    }

    fun allocate(numResources: Int) {
        assert(numResources >= 0)
        assert(numResourcesReady + pendingResources.size + numResources <= maxCapacity)

        pendingResources.addAll(generateSequence { Resource(allocationTime) }.take(numResources))
        checkNewResourcesReady()
    }

    fun deallocate(numResources: Int) {
        assert(numResources >= 0)
        assert(numResources <= numResourcesReady + pendingResources.size)
        (generateSequence { pendingResources.pollLast() } + generateSequence { numResourcesReady-- })
                .take(numResources).count()
    }

    private fun checkNewResourcesReady() {
        val numResourcesReady = pendingResources.takeWhile(Resource::isReady)
        pendingResources.removeAll(numResourcesReady)
        this.numResourcesReady += numResourcesReady.size
    }

    private class Resource(allocationTime: Int) {
        val isReady: Boolean get() = timeBeforeReady == 0
        private var timeBeforeReady: Int = allocationTime

        fun tick() {
            if (!isReady) {
                timeBeforeReady--
            }
        }
    }
}