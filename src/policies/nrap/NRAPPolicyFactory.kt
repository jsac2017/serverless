package policies.nrap

import model.Infrastructure
import model.Policy
import policies.AuxiliaryPolicyParameters
import policies.PolicyFactory

class NRAPPolicyFactory : PolicyFactory {
    override val name: String = "NRAP"

    override fun createInstance(infrastructure: Infrastructure, policyParams: AuxiliaryPolicyParameters): Policy =
            NRAPPolicy(infrastructure)
}