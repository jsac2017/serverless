package policies

import model.Infrastructure
import model.Policy

interface PolicyFactory {
    val name: String

    fun createInstance(infrastructure: Infrastructure, policyParams: AuxiliaryPolicyParameters): Policy
}