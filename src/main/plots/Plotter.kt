package main.plots

import model.*
import policies.AuxiliaryPolicyParameters
import policies.PolicyFactory
import traces.InputGenerationParameters
import traces.TaskEventsSummary
import traces.generateInput
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

data class PlotPoint(
        val inputParams: InputGenerationParameters,
        val modelParameters: ModelParameters,
        val value: Double,
        val policyParams: AuxiliaryPolicyParameters
) {
    private val DECIMAL_FORMAT = DecimalFormat("0.###")
    override fun toString(): String =
            "input=$inputParams, model=$modelParameters, policy=$policyParams, where var = ${DECIMAL_FORMAT.format(value)}"
}

typealias ValueGeneratingFunction = (Accounting, PlotPoint) -> Double

class Plotter(val summaries: Collection<TaskEventsSummary>) {

    enum class RunStrategy(val func: (List<Double>) -> Double) {
        MEAN({it.average()}),
        MEDIAN({it.sorted()[it.size / 2]}),
    }

    var xs: List<PlotPoint> = listOf()

    var numRuns = 1
    var runStrategy = RunStrategy.MEAN
    var policies: List<PolicyFactory> = listOf()
    var numThreads: Int = 1
        set(value) {
            if (value < 1) {
                throw IllegalArgumentException("Number of threads must be at least 1")
            } else {
                field = value
            }
        }
    var estimatorType = OptimalEstimatorType.values()[0]

    fun createPlot(yGen: ValueGeneratingFunction, renderers: Collection<PlotRenderer>) {
        val ys = calculateYs(yGen)
        renderers.forEach { it.plot(xs, policies, ys) }
    }

    private fun calculateYs(yGen: ValueGeneratingFunction): Data {
        val executor = Executors.newFixedThreadPool(numThreads)
        val futures = xs.associateBy({ it }) { point ->
            policies.associateBy({ it }) { policy ->
                executor.submit(Callable<Double> {
                    println("processing $point by ${policy.name}")
                    val values = generateSequence {
                        point.inputParams.generateInput(summaries, Random(point.inputParams.hashCode().toLong()))
                    }.take(numRuns).map {
                        val optimalEstimator = estimatorType.getEstimator(point.modelParameters, point.inputParams)
                        val infra = ConcreteInfrastructure(
                                point.modelParameters, optimalEstimator, point.policyParams, policy
                        )
                        infra.processAll(it.timeSlots)
                        yGen(infra.accounting, point)
                    }.toList()
                    runStrategy.func(values)
                })
            }
        }
        executor.shutdown()
        return futures.mapValues { it.value.mapValues { it.value.get() } }
    }
}



