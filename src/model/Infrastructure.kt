package model

import policies.PolicyFactory
import policies.AuxiliaryPolicyParameters
import utils.ExpirationManager
import kotlin.collections.HashSet


class ConcreteInfrastructure(
        override val parameters: ModelParameters,
        optimalEstimator: OptimalEstimator,
        policyParams: AuxiliaryPolicyParameters,
        policyFactory: PolicyFactory
) : Infrastructure {
    override var currentTimeSlot: Int = 0
        private set
    override val bufferedRequests: Collection<MutableRequest> get() = mutableBufferedRequests
    override val numResourcesAllocated: Int
        get() = resourceManager.numResourcesReady

    val accounting = Accounting(parameters, this, optimalEstimator)

    private val resourceManager = ResourceManager(parameters.maxNumAllocatedResources, parameters.vmAllocationTime)
    private val policy: Policy = policyFactory.createInstance(this, policyParams)
    private val mutableBufferedRequests = HashSet<MutableRequest>()
    private val expirationManager = ExpirationManager<MutableRequest>(expirationTimeFun = {
        if (it.deadline != null)
            it.deadline + it.releaseTime - it.remainingProcessingTime
        else
            null
    })

    fun processAll(input: Collection<Collection<Request>>) {
        input.forEach { this.processTimeSlot(it.map{ it.toMutableRequest(this) }) }
        this.finish()
    }

    private fun processTimeSlot(newRequests: Collection<MutableRequest>) {
        doAdmission(newRequests)
        if (parameters.vmAllocationTime == 0) {
            doPredictionAndAllocation()
        }
        doProcessing()
        dropDueRequests()
        if (parameters.vmAllocationTime > 0) {
            doPredictionAndAllocation()
        }
        resourceManager.tick()
        tick()
    }

    private fun tick() {
        currentTimeSlot++
    }

    private fun finish() {
        accounting.finish()
        while (bufferedRequests.isNotEmpty()) {
            processTimeSlot(listOf())
        }
    }

    private fun doProcessing() {
        accounting.chargeMaintenance(resourceManager.numResourcesReady)

        val requestsToProcess = policy.selectForProcessing()
        assert(requestsToProcess.size <= numResourcesAllocated)
        assert(bufferedRequests.containsAll(requestsToProcess))

        requestsToProcess.forEach(MutableRequest::process)
        policy.handleProcessingFinished()

        val completedRequests = requestsToProcess.filter { it.remainingProcessingTime == 0 }
        accounting.accountForCompleted(completedRequests)

        mutableBufferedRequests.removeAll(completedRequests)
        policy.handleBufferLeave(completedRequests, ReasonToLeave.COMPLETED)

    }

    private fun doAdmission(arrived: Collection<MutableRequest>) {
        accounting.noteArrived(arrived)
        arrived.forEach { expirationManager.watchForExpiration(it) }

        val droppedRequests = policy.selectForDrop(arrived)
        assert(parameters.bufferSize == null ||
                bufferedRequests.size + arrived.size - droppedRequests.size <= parameters.bufferSize)
        assert(droppedRequests.all { bufferedRequests.contains(it) || arrived.contains(it) })
        accounting.noteDropped(droppedRequests)
        mutableBufferedRequests.addAll(arrived)
        mutableBufferedRequests.removeAll(droppedRequests)
    }

    private fun doPredictionAndAllocation() {
        accounting.chargeAllocation(Math.max(0, resourceManager.requestCapacity(policy.predictProcessingCapacity())))
    }

    private fun dropDueRequests() {
        expirationManager.tick()
        val dueRequests = expirationManager.pollExpired().filter { bufferedRequests.contains(it) }
        if (dueRequests.isNotEmpty()) {
            accounting.noteDropped(dueRequests)
            mutableBufferedRequests.removeAll(dueRequests)
            policy.handleBufferLeave(dueRequests, ReasonToLeave.DUE)
        }
    }
}

