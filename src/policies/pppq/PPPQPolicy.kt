package policies.pppq

import model.*
import policies.*
import utils.ExpirationManager
import utils.PointedPQ
import java.util.Comparator
import java.util.TreeSet

class PPPQPolicy (
        private val infra: Infrastructure,
        private val parameters: PPPQParameters,
        workConservative: Boolean
) : Policy {
    private val comparator: Comparator<Request> = compareBy({-hotUnitValue(it, infra.parameters) }, Request::id)

    private val queue = PointedPQ<MutableRequest>(comparator, infra.parameters.maxNumAllocatedResources - 1)
    private val workConservativeQueue = if (workConservative) TreeSet<MutableRequest>(comparator) else null
    private val pppqExpiration = ExpirationManager<MutableRequest>(expirationTimeFun = { pppqExpirationTime(it) })
    private val queueCandidates = TreeSet<MutableRequest>(compareBy({-coldUnitValue(it, infra.parameters) }, Request::id))

    override fun selectForDrop(arrivingRequests: Collection<MutableRequest>): Collection<MutableRequest> {
        val (accept, drop) = arrivingRequests.partition { hotUnitValue(it, infra.parameters) > 0 }
                .run { Pair(first, second.toMutableList()) }

        accept.forEach {
            queueCandidates.add(it)
            workConservativeQueue?.add(it)
            pppqExpiration.watchForExpiration(it)
        }

        pppqExpiration.pollExpired().filter { infra.bufferedRequests.contains(it) }.forEach {
            drop.add(it)
            queue.remove(it)
            workConservativeQueue?.remove(it)
            queueCandidates.remove(it)
        }

        while (queueCandidates.isNotEmpty() && checkPreemptionCondition(queueCandidates.first())) {
            val candidate = queueCandidates.pollFirst()
            if (checkPessimisticCondition(candidate)) {
                queue.add(candidate)
                workConservativeQueue?.remove(candidate)
            }
        }

        return drop
    }

    override fun selectForProcessing(): Collection<MutableRequest> {
        val nonWCResult = queue.take(infra.numResourcesAllocated)
        if (workConservativeQueue != null) {
            return nonWCResult + workConservativeQueue.take(infra.numResourcesAllocated - nonWCResult.size)
        } else {
            return nonWCResult
        }
    }

    override fun handleProcessingFinished() {
        pppqExpiration.tick()
    }

    override fun predictProcessingCapacity(): Int {
        val nonWorkConservative = Math.min(queue.size, infra.parameters.maxNumAllocatedResources)
        if (nonWorkConservative < infra.numResourcesAllocated && workConservativeQueue != null) {
            return Math.min(nonWorkConservative + workConservativeQueue.size, infra.numResourcesAllocated)
        } else {
            return nonWorkConservative
        }
    }

    override fun handleBufferLeave(leavingRequests: Collection<MutableRequest>, reason: ReasonToLeave) {
        leavingRequests.forEach {
            if (!queue.remove(it)) {
                workConservativeQueue?.remove(it)
                queueCandidates.remove(it)
            }
        }
    }

    private fun checkPreemptionCondition(request: Request): Boolean {
        val worstActiveRequest = queue.pointed
        return worstActiveRequest == null || coldUnitValue(request, infra.parameters) >= 0 &&
                coldUnitValue(request, infra.parameters) >=
                        parameters.preemptionFactor * coldUnitValue(worstActiveRequest, infra.parameters)
    }

    private fun checkPessimisticCondition(request: MutableRequest): Boolean =
            request.currentDeadline == null ||
                    request.currentDeadline!! >= (1 + parameters.pessimisticFactor) * request.initialProcessingTime + infra.parameters.vmAllocationTime

    private fun pppqExpirationTime(request: MutableRequest): Int? =
            if (request.deadline == null) null else
                request.releaseTime + request.deadline - request.remainingProcessingTime - infra.parameters.vmAllocationTime
}

