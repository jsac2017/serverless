package model

import java.text.DecimalFormat

open class Request(
        val id: Int,
        val releaseTime: Int,
        val deadline: Int?,
        val value: Double,
        val initialProcessingTime: Int
) {
    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is Request && id == other.id
    }
}

fun Request.toMutableRequest(infra: Infrastructure): MutableRequest {
    return MutableRequest(this, infra)
}

class MutableRequest(
        request: Request,
        val infra: Infrastructure
) : Request(request.id, request.releaseTime, request.deadline, request.value, request.initialProcessingTime) {
    var remainingProcessingTime: Int = initialProcessingTime
        private set
    val currentDeadline: Int? get() = if (deadline == null) null else releaseTime + deadline - infra.currentTimeSlot

    fun process() {
        assert(remainingProcessingTime > 0)
        remainingProcessingTime--
    }
}

data class ModelParameters (
        val vmAllocationTime: Int,
        val maxNumAllocatedResources: Int,
        val allocationCost: Double,
        val maintenanceCost: Double,
        val bufferSize: Int?
) {
    override fun toString(): String {
        val format = DecimalFormat("0.###")
        return "(" +
                    "f=${vmAllocationTime}dT" +
                    ",R=$maxNumAllocatedResources" +
                    ",a=${format.format(allocationCost)}" +
                    ",m=${format.format(maintenanceCost)}/dT" +
                    ",B=${bufferSize ?: "infty"}" +
                ")"
    }
}


interface Infrastructure {
    val parameters: ModelParameters

    val numResourcesAllocated: Int
    val currentTimeSlot: Int
    val bufferedRequests: Collection<MutableRequest>
}

enum class ReasonToLeave {
    DUE, COMPLETED
}

interface Policy {
    fun selectForDrop(arrivingRequests: Collection<MutableRequest>): Collection<MutableRequest>
    fun selectForProcessing(): Collection<MutableRequest>
    fun handleProcessingFinished()
    fun predictProcessingCapacity(): Int

    fun handleBufferLeave(leavingRequests: Collection<MutableRequest>, reason: ReasonToLeave)
}
