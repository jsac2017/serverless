package model

import policies.hotUnitValue
import traces.InputGenerationParameters
import java.util.TreeSet

interface OptimalEstimator {
    fun noteArrived(arrivedRequests: Collection<MutableRequest>)
    fun finish()
    val optimalUpperBound: Double
}

enum class OptimalEstimatorType {
    SMART {
        override fun getEstimator(modelParameters: ModelParameters, inputParameters: InputGenerationParameters): OptimalEstimator =
            if (inputParameters.unitProcessing) {
                OnlyValueEstimator(modelParameters, modelParameters.bufferSize != null)
            } else {
                SimpleEstimator(modelParameters)
            }
    },
    SIMPLE {
        override fun getEstimator(modelParameters: ModelParameters, inputParameters: InputGenerationParameters): OptimalEstimator =
            SimpleEstimator(modelParameters)
    },
    ONLY_VALUE_BUFFER {
        override fun getEstimator(modelParameters: ModelParameters, inputParameters: InputGenerationParameters): OptimalEstimator =
                OnlyValueEstimator(modelParameters, true)
    },
    ONLY_VALUE_NO_BUFFER {
        override fun getEstimator(modelParameters: ModelParameters, inputParameters: InputGenerationParameters): OptimalEstimator =
                OnlyValueEstimator(modelParameters, false)
    };

    abstract fun getEstimator(modelParameters: ModelParameters, inputParameters: InputGenerationParameters): OptimalEstimator
}


class SimpleEstimator(val parameters: ModelParameters) : OptimalEstimator {
    override var optimalUpperBound: Double = 0.0
        private set

    override fun noteArrived(arrivedRequests: Collection<MutableRequest>) {
        optimalUpperBound += arrivedRequests.map {
            hotUnitValue(it, parameters) * it.initialProcessingTime
        }.filter { it > 0 }.sum()
    }

    override fun finish() { }
}

class OnlyValueEstimator(val parameters: ModelParameters, private val limitBuffer: Boolean) : OptimalEstimator {
    override var optimalUpperBound: Double = 0.0
        private set
    private val queue = TreeSet<MutableRequest>(compareBy({-hotUnitValue(it, parameters)}, Request::id))

    override fun noteArrived(arrivedRequests: Collection<MutableRequest>) {
        queue.addAll(arrivedRequests.filter { hotUnitValue(it, parameters) > 0 })
        while (limitBuffer && parameters.bufferSize != null && queue.size > parameters.bufferSize) {
            queue.pollLast()
        }
       optimalUpperBound += generateSequence { queue.pollFirst() }
               .take(parameters.maxNumAllocatedResources)
               .sumByDouble { hotUnitValue(it, parameters) * it.initialProcessingTime }
    }

    override fun finish() {
        optimalUpperBound += generateSequence { queue.pollFirst() }
                .sumByDouble { hotUnitValue(it, parameters) * it.initialProcessingTime }
    }

}

