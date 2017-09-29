package policies.pq

import model.Infrastructure
import model.MutableRequest
import model.Policy
import policies.AuxiliaryPolicyParameters
import policies.PolicyFactory
import java.util.Comparator

class PQPolicyFactory(
        comparatorName: String,
        private val cmp: Comparator<MutableRequest>
) : PolicyFactory {
    override val name = "PQ($comparatorName)"

    override fun createInstance(infrastructure: Infrastructure, policyParams: AuxiliaryPolicyParameters): Policy =
            PQPolicy(infrastructure, cmp)
}