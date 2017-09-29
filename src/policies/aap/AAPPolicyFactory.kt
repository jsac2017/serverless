package policies.aap

import model.Infrastructure
import model.Policy
import policies.AuxiliaryPolicyParameters
import policies.PolicyFactory
import java.text.DecimalFormat

class AAPPolicyFactory(private val amortizationFactor: Double) : PolicyFactory {
    private val format = DecimalFormat("0.##")
    override val name: String = "AAP(${format.format(amortizationFactor)})"


    override fun createInstance(infrastructure: Infrastructure, policyParams: AuxiliaryPolicyParameters): Policy {
        if (amortizationFactor <= 1.0) {
            throw IllegalStateException("amortization factor must be greater than one, but was $amortizationFactor")
        }
        if (policyParams.minimalValue <= infrastructure.parameters.maintenanceCost) {
            throw IllegalStateException("hot unit value must be grater than zero")
        }

        return AAPPolicy(infrastructure, policyParams.minimalValue, amortizationFactor)
    }
}

class AAPPolicyVarFactory: PolicyFactory {
    override val name: String = "AAP(var)"

    override fun createInstance(infrastructure: Infrastructure, policyParams: AuxiliaryPolicyParameters): Policy {
        val amortizationFactor = policyParams.policyParam ?: throw IllegalStateException("Policy parameter must be set")
        if (amortizationFactor <= 1.0) {
            throw IllegalStateException("amortization factor must be greater than one, but was $amortizationFactor")
        }
        if (policyParams.minimalValue <= infrastructure.parameters.maintenanceCost) {
            throw IllegalStateException("hot unit value must be grater than zero")
        }
        return AAPPolicy(infrastructure, policyParams.minimalValue, amortizationFactor)
    }
}