package policies.nrap

import model.Infrastructure
import model.MutableRequest
import model.Policy
import model.ReasonToLeave
import policies.hotUnitValue

class NRAPPolicy(private val infra: Infrastructure) : Policy {
    override fun selectForDrop(arrivingRequests: Collection<MutableRequest>): Collection<MutableRequest> {
        val (accept, drop) = arrivingRequests.partition { hotUnitValue(it, infra.parameters) > 0 }
        if (infra.parameters.bufferSize != null) {
            return drop + accept.drop(infra.parameters.bufferSize!! - infra.bufferedRequests.size)
        } else {
            return drop
        }
    }

    override fun selectForProcessing(): Collection<MutableRequest> =
        infra.bufferedRequests.take(infra.numResourcesAllocated)

    override fun handleProcessingFinished() { }

    override fun predictProcessingCapacity(): Int =
        Math.min(infra.bufferedRequests.size, infra.parameters.maxNumAllocatedResources)

    override fun handleBufferLeave(leavingRequests: Collection<MutableRequest>, reason: ReasonToLeave) { }
}
