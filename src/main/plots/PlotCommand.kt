package main.plots

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.beust.jcommander.converters.PathConverter
import main.cli.InputArgs
import main.cli.ModelArgs
import main.cli.PolicyArgs
import model.OptimalEstimatorType
import policies.PolicyFactory
import utils.Range
import utils.timeSlotsToSeconds
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.DecimalFormat
import kotlin.system.exitProcess

typealias Data = Map<PlotPoint, Map<PolicyFactory, Double>>

@Parameters(commandDescription = "Plot some data")
object PlotCommand {
    private val DECIMAL_FORMAT = DecimalFormat("0.###")

    val name = "plot"

    @Parameter(names = arrayOf("--value", "-y"), required = true)
    private var valueName: String? = null

    @Parameter(names = arrayOf("--dir"), description = "directory, where to save plots", converter = PathConverter::class)
    private var dir: Path = Paths.get("plots")

    @Parameter(names = arrayOf("--runs-per-input"), description = "Number of runs for each input parameter (random)")
    private var runsPerInput: Int = 1

    @Parameter(names = arrayOf("--num-threads"), description = "the number of threads to use")
    private var numThreads: Int = 1

    @Parameter(names = arrayOf("--run-strategy"), description = "How to combine multiple runs")
    private var runStrategy: String = "mean"

    fun run(inputArgs: InputArgs, modelArgs: ModelArgs, policyArgs: PolicyArgs, policies: List<PolicyFactory>, estimatorType: OptimalEstimatorType) {
        // initialize these here for early error detection
        val yGen = getValueGeneratingFunction()

        val plotter = Plotter(inputArgs.readSummaries())
        plotter.xs = generatePoints(inputArgs, modelArgs, policyArgs)
        plotter.policies = policies
        plotter.numThreads = numThreads
        plotter.numRuns = runsPerInput
        plotter.runStrategy = Plotter.RunStrategy.valueOf(runStrategy.toUpperCase())
        plotter.estimatorType = estimatorType

        val tableRenderer = TableRenderer(margin = 2)
        val baseFileName = generateFileName(inputArgs, modelArgs, policyArgs)

        if (!Files.exists(dir)) {
            Files.createDirectory(dir)
        }

        plotter.createPlot(yGen, listOf(
                TSVRenderer(dir.resolve("$baseFileName.t.tsv"), policiesInHeader = false),
                TSVRenderer(dir.resolve("$baseFileName.tsv"), policiesInHeader = true),
                tableRenderer
        ))

        println(tableRenderer.lastPlot)
    }

    private fun getValueGeneratingFunction(): ValueGeneratingFunction =
        if (ALL_VALUES.containsKey(valueName)) {
            ALL_VALUES[valueName]!!
        } else {
            System.err.println("Unknown value name: $valueName")
            System.err.println("Available values: " + ALL_VALUES.keys.joinToString(", "))
            exitProcess(1)
        }

    private val ALL_VALUES = mapOf<String, ValueGeneratingFunction> (
            "objective" to { accnt, _ -> accnt.objective },
            "objective-percentage" to { accnt, _ -> accnt.objectivePercentage },
            "average-latency" to { accnt, point ->  point.inputParams.timeSlotsToSeconds(accnt.averageLatency) },
            "opt" to { accnt, _ -> accnt.optimalUpperBound }
    )

    private fun generateFileName(inputArgs: InputArgs, modelArgs: ModelArgs, policyArgs: PolicyArgs): String {
        val entryList = mutableListOf<String>()
        appendInputGenerationEntries(entryList, inputArgs)
        appendModelParametersEntries(entryList, modelArgs)
        appendPolicyParamatersEntries(entryList, policyArgs)
        appendPlotEntries(entryList)
        return entryList.joinToString("_")
    }

    private fun Range.plotFormat() = format(DECIMAL_FORMAT, "-")

    private fun appendPolicyParamatersEntries(entryList: MutableList<String>, policyArgs: PolicyArgs) {
        if (policyArgs.policyParam != null) {
            entryList.add("pp${policyArgs.policyParam!!.plotFormat()}")
        }
    }

    private fun appendPlotEntries(entryList: MutableList<String>) {
        entryList.add("$valueName")
    }

    private fun appendModelParametersEntries(entryList: MutableList<String>, modelArgs: ModelArgs) {
        entryList.add("f${modelArgs.vmAllocationTime.plotFormat()}")
        entryList.add("R${modelArgs.maxNumAllocatedResources.plotFormat()}")
        entryList.add("a${modelArgs.allocationCost.plotFormat()}")
        entryList.add("m${modelArgs.maintenanceCost.plotFormat()}")
        if (modelArgs.bufferSize != null) {
            entryList.add("B${modelArgs.bufferSize!!.plotFormat()}")
        }
    }

    private fun appendInputGenerationEntries(entryList: MutableList<String>, inputArgs: InputArgs) {
        entryList.add(inputArgs.summaries?.fileName.toString())
        entryList.add("dt${inputArgs.timeSlotDuration.plotFormat()}")
        if (!inputArgs.uniformValue) {
            entryList.add("vs${inputArgs.valueScale.plotFormat()}")
            if (inputArgs.valueExponent != null) {
                entryList.add("vexp${DECIMAL_FORMAT.format(inputArgs.valueExponent!!)}")
            }
        }
        if (!inputArgs.uniformProcessing) {
            entryList.add("ws${inputArgs.processingScale.plotFormat()}")
        }
        if (!inputArgs.schedulingCushionsDisable) {
            entryList.addAll(inputArgs.schedulingClassCushions.zip('a'..'d').map {
                (v, i) ->
                "s$i${DECIMAL_FORMAT.format(v)}"
            })
            entryList.add("ss${inputArgs.schedulingClassCushionsScale.plotFormat()}")
            if (inputArgs.schedulingCushionsRandomized) {
                entryList.add("ssrnd$runsPerInput")
            }
        }
    }

    private fun generatePoints(inputArgs: InputArgs, modelArgs: ModelArgs, policyArgs: PolicyArgs): List<PlotPoint> =
            inputArgs.startUnit { inputParameters ->
                modelArgs.then(inputParameters)  { modelParameters ->
                    policyArgs.then(inputParameters) { policyParams, v ->
                        if (v == null) {
                            throw IllegalStateException("At least one parameter must vary!")
                        }
                        PlotPoint(inputParameters, modelParameters, v, policyParams)
                    }
                }
            }
}

