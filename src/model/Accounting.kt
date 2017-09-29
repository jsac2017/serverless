package model

class Accounting(
        val parameters: ModelParameters,
        private val infra: Infrastructure,
        private val optimalEstimator: OptimalEstimator
) {
    var numAllocations: Int = 0
        private set
    var totalMaintenanceTime: Int = 0
        private set
    var numRequestsCompleted: Int = 0
        private set
    var numRequestsDropped: Int = 0
        private set
    var totalValueCompleted: Double = 0.0
        private set
    var totalLatency: Double = 0.0
        private set

    val optimalUpperBound: Double get() = optimalEstimator.optimalUpperBound

    val cost: Double get() = numAllocations * parameters.allocationCost + totalMaintenanceTime * parameters.maintenanceCost
    val revenue: Double get() = totalValueCompleted
    val objective: Double get() = revenue - cost
    val objectivePercentage: Double get() = objective / optimalUpperBound
    val averageLatency: Double get() = totalLatency / (numRequestsCompleted + numRequestsDropped)

    fun chargeAllocation(numberOfAllocatedResources: Int) {
        numAllocations += numberOfAllocatedResources
    }

    fun chargeMaintenance(numberOfActiveResources: Int) {
        totalMaintenanceTime += numberOfActiveResources
    }

    fun noteArrived(arrivedRequests: Collection<MutableRequest>) {
        optimalEstimator.noteArrived(arrivedRequests)
    }

    fun finish() {
        optimalEstimator.finish()
    }

    fun accountForCompleted(completedRequests: Collection<MutableRequest>) {
        totalValueCompleted += completedRequests.map(Request::value).sum()
        numRequestsCompleted += completedRequests.size
        totalLatency += completedRequests.map { infra.currentTimeSlot - it.releaseTime  }.sum()
    }

    fun noteDropped(droppedRequests: Collection<MutableRequest>) {
        numRequestsDropped += droppedRequests.size
        totalLatency += droppedRequests.map { infra.currentTimeSlot - it.releaseTime }.sum()
    }
}