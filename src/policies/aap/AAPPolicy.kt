package policies.aap

import model.Infrastructure
import model.MutableRequest
import model.Policy
import model.ReasonToLeave
import utils.SumPQ
import java.util.*


class AAPPolicy(private val infra: Infrastructure, minValue: Double, amortizationFactor: Double) : Policy {
    private val threshold = amortizationFactor * infra.parameters.allocationCost / (1 - infra.parameters.maintenanceCost / minValue)
    private val comparator = compareBy<MutableRequest>{ it.value }.reversed().thenBy { it.id }
    private val waitingRequests = SumPQ(comparator, MutableRequest::value)
    private val assignedRequests = TreeSet(comparator)
    private var inactiveTimeSlotsInARow = 0

    override fun selectForDrop(arrivingRequests: Collection<MutableRequest>): Collection<MutableRequest> {
        arrivingRequests.forEach {
            if (assignedRequests.isNotEmpty() && comparator.compare(it, assignedRequests.last()) < 0) {
                assignedRequests.add(it)
                waitingRequests.add(assignedRequests.pollLast())
            } else {
                waitingRequests.add(it)
            }
        }

        var droppedRequests = dropToFitIntoCapacity()
        fillUnassignedResources()

        if (assignedRequests.isEmpty() && arrivingRequests.isEmpty()) {
            droppedRequests += dropIfInactive()
        } else {
            noticeActiveTimeSlot()
        }

        return droppedRequests
    }

    override fun selectForProcessing(): Collection<MutableRequest> =
        generateSequence { assignedRequests.pollFirst() }.take(infra.numResourcesAllocated).toList()

    override fun handleProcessingFinished() { }

    override fun predictProcessingCapacity(): Int {
        fillUnassignedResources()
        var nextProcessingCapacity = Math.min(infra.numResourcesAllocated, assignedRequests.size)
        while (nextProcessingCapacity < infra.parameters.maxNumAllocatedResources &&
                (threshold == 0.0 && waitingRequests.size > 0 || waitingRequests.sum > threshold)) {
            var batchValue = 0.0
            while (batchValue < threshold) {
                val nextBatchRequest = waitingRequests.pollFirst()!!
                assignedRequests.add(nextBatchRequest)
                batchValue += nextBatchRequest.value
            }
            nextProcessingCapacity++
        }
        return nextProcessingCapacity
    }

    override fun handleBufferLeave(leavingRequests: Collection<MutableRequest>, reason: ReasonToLeave) {
        if (reason == ReasonToLeave.DUE) {
            throw IllegalStateException("AAP policy does not support deadlines")
        }
    }

    private fun fillUnassignedResources() {
        if (assignedRequests.size < infra.numResourcesAllocated) {
            assignedRequests.addAll(generateSequence { waitingRequests.pollFirst() }.take(infra.numResourcesAllocated - assignedRequests.size))
        }
    }

    private fun dropToFitIntoCapacity(): List<MutableRequest> {
        val bufferLimit = infra.parameters.bufferSize ?: return listOf()

        val droppedRequests = mutableListOf<MutableRequest>()
        while (assignedRequests.size + waitingRequests.size > bufferLimit) {
            val droppedRequest = waitingRequests.pollLast()!!
            droppedRequests.add(droppedRequest)
        }
        return droppedRequests
    }

    private fun dropIfInactive(): List<MutableRequest> {
        inactiveTimeSlotsInARow++
        if (inactiveTimeSlotsInARow == Math.ceil(threshold).toInt()) {
            val result = waitingRequests.toList()
            waitingRequests.clear()
            return result
        }
        return emptyList()
    }

    private fun noticeActiveTimeSlot() {
        inactiveTimeSlotsInARow = 0
    }
}

