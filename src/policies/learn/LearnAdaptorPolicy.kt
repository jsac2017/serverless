package policies.learn

import model.Infrastructure
import model.MutableRequest
import model.Policy
import model.ReasonToLeave
import policies.pq.PQPolicy
import java.util.*

class LearnAdaptorPolicy(
        private val infrastructure: Infrastructure,
        private val underlyingPolicy: PQPolicy,
        private val learningMethod: LearningMethod,
        private val learningTime: Int) : Policy {
    override fun selectForDrop(arrivingRequests: Collection<MutableRequest>): Collection<MutableRequest> {
        return underlyingPolicy.selectForDrop(arrivingRequests)
    }

    override fun selectForProcessing(): Collection<MutableRequest> {
        return underlyingPolicy.selectForProcessing()
    }

    override fun handleProcessingFinished() {
        underlyingPolicy.handleProcessingFinished()
    }

    override fun handleBufferLeave(leavingRequests: Collection<MutableRequest>, reason: ReasonToLeave) {
        underlyingPolicy.handleBufferLeave(leavingRequests, reason)
    }

    enum class LearningMethod(val stat: (Collection<Int>) -> Int) {
        AVERAGE({it.average().toInt()}),
        MEDIAN({it.sorted()[it.size / 2]})
    }

    private val history = ArrayDeque<Int>()

    override fun predictProcessingCapacity(): Int {
        history.add(underlyingPolicy.predictProcessingCapacity())
        if (history.size > learningTime) {
            history.pollFirst()
        }
        return Math.max(learningMethod.stat(history), Math.min(infrastructure.numResourcesAllocated, infrastructure.bufferedRequests.size))
    }
}

