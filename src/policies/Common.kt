package policies

import model.*
import utils.optional
import java.text.DecimalFormat

data class AuxiliaryPolicyParameters(
        val minimalDeadlineCushion: Double?,
        val minimalValue: Double,
        val policyParam: Double? = null) {
    private val format = DecimalFormat("0.###")
    override fun toString(): String =
            "(" +
                "minv=${format.format(minimalValue)}" +
                optional(minimalDeadlineCushion != null) {",minc=${format.format(minimalDeadlineCushion)}"} +
                optional(policyParam != null) {",policy=${format.format(policyParam)}"} +
            ")"
}

fun hotUnitValue(request: Request, params: ModelParameters): Double {
    return request.value / request.initialProcessingTime - params.maintenanceCost
}

fun coldUnitValue(request: Request, params: ModelParameters): Double {
    return (request.value - params.allocationCost) / request.initialProcessingTime - params.maintenanceCost
}

