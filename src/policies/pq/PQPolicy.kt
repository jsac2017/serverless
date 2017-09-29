package policies.pq

import model.Infrastructure
import model.MutableRequest
import model.Policy
import model.ReasonToLeave
import policies.hotUnitValue
import java.util.Comparator
import java.util.TreeSet

class PQPolicy(private val infra: Infrastructure, cmp: Comparator<MutableRequest>) : Policy {
    private val scheduledForProcessing = arrayListOf<MutableRequest>()
    private val queue = TreeSet<MutableRequest>(cmp.thenComparingInt { it.id })

    override fun selectForDrop(arrivingRequests: Collection<MutableRequest>): Collection<MutableRequest> {
        val (accept, drop) = arrivingRequests.partition { hotUnitValue(it, infra.parameters) > 0 }
        queue.addAll(accept)
        return drop + dropToFitCapacity()
    }

    override fun selectForProcessing(): Collection<MutableRequest> {
        scheduledForProcessing.addAll(
            generateSequence { queue.pollFirst() }.take(infra.numResourcesAllocated)
        )
        return scheduledForProcessing.toList()
    }

    override fun handleProcessingFinished() {
        queue.addAll(scheduledForProcessing)
        scheduledForProcessing.clear()
    }

    override fun predictProcessingCapacity(): Int {
        return Math.min(infra.bufferedRequests.size, infra.parameters.maxNumAllocatedResources)
    }

    override fun handleBufferLeave(leavingRequests: Collection<MutableRequest>, reason: ReasonToLeave) {
        queue.removeAll(leavingRequests)
    }

    private fun dropToFitCapacity(): Collection<MutableRequest> {
        val limit = infra.parameters.bufferSize ?: return listOf()
        return generateSequence { queue.pollLast() }.take(Math.max(0,queue.size - limit)).toList()
    }
}
