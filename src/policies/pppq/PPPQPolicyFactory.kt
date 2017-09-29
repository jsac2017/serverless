package policies.pppq

import model.Infrastructure
import model.Policy
import policies.AuxiliaryPolicyParameters
import policies.PolicyFactory
import utils.optional

abstract class PPPQPolicyFactory(val workConservative: Boolean): PolicyFactory {
    protected abstract val subName: String

    override val name get() = "PPPQ($subName" + optional(workConservative) { ":wc" } + ")"

    companion object {
        fun fromParameters(pessimisticFactor: Double, preemptionFactor: Double, workConservative: Boolean = false): PPPQPolicyFactory =
                DirectParametersPPPQPolicyFactory(PPPQParameters(pessimisticFactor, preemptionFactor), workConservative)

        fun inputOptimalCushion(workConservative: Boolean = false) : PPPQPolicyFactory =
                OptimalPPPQPolicyFactory(workConservative)

        fun fromDeadlineCushion(deadlineCushion: Double, workConservative: Boolean = false): PPPQPolicyFactory =
                CushionPPPQPolicyFactory(deadlineCushion, workConservative)

        fun variableCushion(workConservative: Boolean = false): PPPQPolicyFactory =
                CustomDeadlineCushionPPPQPolicyFactory(workConservative)
    }
}

private class DirectParametersPPPQPolicyFactory(
        private val parameters: PPPQParameters,
        workConservative: Boolean
) : PPPQPolicyFactory(workConservative) {
    override val subName: String = "${parameters.pessimisticFactor}:${parameters.preemptionFactor}"

    override fun createInstance(infrastructure: Infrastructure, policyParams: AuxiliaryPolicyParameters): Policy =
            PPPQPolicy(infrastructure, parameters, workConservative)
}

private class CushionPPPQPolicyFactory(
        private val deadlineCushion: Double, workConservative: Boolean
) : PPPQPolicyFactory(workConservative) {
    override val subName: String = "$deadlineCushion"

    override fun createInstance(infrastructure: Infrastructure, policyParams: AuxiliaryPolicyParameters): Policy =
            PPPQPolicy(infrastructure, PPPQParameters.fromDeadlineCushion(deadlineCushion), workConservative)
}

private class OptimalPPPQPolicyFactory(workConservative: Boolean) : PPPQPolicyFactory(workConservative) {
    override val subName: String = "opt"

    override fun createInstance(infrastructure: Infrastructure, policyParams: AuxiliaryPolicyParameters): Policy =
            PPPQPolicy(infrastructure, PPPQParameters.fromDeadlineCushion(policyParams.minimalDeadlineCushion!!), workConservative)
}

private class CustomDeadlineCushionPPPQPolicyFactory(workConservative: Boolean) : PPPQPolicyFactory(workConservative) {
    override val subName: String = "var"

    override fun createInstance(infrastructure: Infrastructure, policyParams: AuxiliaryPolicyParameters): Policy =
            PPPQPolicy(infrastructure, PPPQParameters.fromDeadlineCushion(policyParams.policyParam!!), workConservative)
}