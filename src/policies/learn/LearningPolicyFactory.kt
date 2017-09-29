package policies.learn

import model.Infrastructure
import model.MutableRequest
import model.Policy
import policies.AuxiliaryPolicyParameters
import policies.PolicyFactory
import policies.pq.PQPolicy

class LearningPolicyFactory(
        comparatorName: String,
        private val comparator: Comparator<MutableRequest>,
        private val learningMethod: LearnAdaptorPolicy.LearningMethod,
        private val learningTime: Int) : PolicyFactory {
    // TODO: learning time in seconds?

    override val name: String = "Learn($comparatorName:${learningMethod.toString().toLowerCase()}:$learningTime)"
    override fun createInstance(infrastructure: Infrastructure, policyParams: AuxiliaryPolicyParameters): Policy =
        LearnAdaptorPolicy(infrastructure, PQPolicy(infrastructure, comparator), learningMethod, learningTime)
}