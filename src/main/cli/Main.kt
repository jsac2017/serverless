package main.cli

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.beust.jcommander.converters.PathConverter
import main.plots.PointGenerator
import utils.minimalCushion
import utils.perSecondToPerTimeSlot
import utils.secondsToTimeSlots
import main.plots.PlotCommand
import main.plots.startUnit
import utils.Range
import traces.*
import model.ConcreteInfrastructure
import model.ModelParameters
import model.OptimalEstimatorType
import policies.AuxiliaryPolicyParameters
import policies.PolicyFactory
import policies.PolicyParseDescription
import utils.printInputStats
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

class GenericArgs {
    class UnknownPolicyException(val policyName: String) : IllegalArgumentException("Unknown policy: $policyName")
    class UnknownOptimalEstimatorException(val estimator: String) : IllegalArgumentException("Unknown estimator: $estimator")

    @Parameter(names = arrayOf("--help"), help = true)
    var help: Boolean = false

    @Parameter(names = arrayOf("--policy"), description = "Which policies to run")
    var policies: List<String> = arrayListOf("NRAP", "PQ(v/w)", "PPPQ(50:wc)", "PPPQ(10:wc)", "PPPQ(10)", "PPPQ(50)")

    @Parameter(names = arrayOf("--list-policies"), description = "List of the known policies")
    var listPolicies = false

    @Parameter(names = arrayOf("--estimator"))
    var estimator: String = OptimalEstimatorType.values()[0].name.toLowerCase()

    fun getPolicyFactories(): List<PolicyFactory> =
        policies.asSequence().map { name ->
                PolicyParseDescription.createFactory(name) ?: run {
                    throw UnknownPolicyException(name)
                }
        }.toList()

    fun getEstimatorType(): OptimalEstimatorType =
        try {
            OptimalEstimatorType.valueOf(estimator.toUpperCase())
        } catch (e: IllegalArgumentException) {
            throw UnknownOptimalEstimatorException(estimator)
        }
}

class InputArgs : PointGenerator<Unit, InputGenerationParameters>() {
    @Parameter(names = arrayOf("--time-slot-duration"), description = "The duration of a time slot in seconds", converter = RangeConverter::class)
    var timeSlotDuration: Range = Range.point(1.0)

    @Parameter(names = arrayOf("--processing-scale"), description = "The factor by which to scale processing time", converter = RangeConverter::class)
    var processingScale: Range = Range.point(1.0)

    @Parameter(names = arrayOf("--processing-uniform"), description = "Every request can be processed in a single time slot")
    var uniformProcessing: Boolean = false

    @Parameter(names = arrayOf("--value-scale"), description = "The factor by which to scale priority", converter = RangeConverter::class)
    var valueScale: Range = Range.point(1.0)

    @Parameter(names = arrayOf("--value-exponent"), description = "The base for exponential values")
    var valueExponent: Double? = null

    @Parameter(names = arrayOf("--value-uniform"), description = "Whether to use uiniform values?")
    var uniformValue: Boolean = false

    @Parameter(names = arrayOf("--deadline-cushions"), arity = 4, description = "The multiplicative cushions for a deadline")
    var schedulingClassCushions: List<Double> = arrayListOf(1.0, 2.0, 4.0, 10.0)

    @Parameter(names = arrayOf("--deadline-cushions-scale"), description = "The multiplicative factor for easy deadline adjustments", converter = RangeConverter::class)
    var schedulingClassCushionsScale: Range = Range.point(1.0)

    @Parameter(names = arrayOf("--deadline-random"), description = "Whether to generate deadlines randomly?")
    var schedulingCushionsRandomized: Boolean = false

    @Parameter(names = arrayOf("--deadline-disable"), description = "Do not use deadline")
    var schedulingCushionsDisable: Boolean = false

    @Parameter(names = arrayOf("--summaries"), description = "Serialized summaries", required = true, converter = PathConverter::class)
    var summaries: Path? = null

    init {
        addProp({timeSlotDuration}) { x, _, input -> input.copy(timeSlotDuration = (x * InputGenerationParameters.ONE_SECOND).toLong()) }
        addProp({processingScale}) { x, _, input -> input.copy(processingScale = x)}
        addProp({schedulingClassCushionsScale}) { x, _, input -> input.copy(deadlines = input.deadlines?.copy(cushionsScale = x))}
        addProp({valueScale}) { x, _, input -> input.copy(valueScale = x)}
    }

    override fun initialParameters(value: Unit) = InputGenerationParameters(
            timeSlotDuration = InputGenerationParameters.ONE_SECOND,
            processingScale = 1.0,
            unitProcessing = uniformProcessing,
            unitValue = uniformValue,
            deadlines = if (schedulingCushionsDisable) null else DeadlineParameters(
               schedulingClassCushions = schedulingClassCushions,
               isRandomized = schedulingCushionsRandomized,
               cushionsScale = 1.0
            ),
            valueScale = 1.0,
            valueExpBase = valueExponent
    )

    fun toInputGenerationParameters() = startUnit { x, _ -> x }.single()

    fun readSummaries(): Collection<TaskEventsSummary> {
        return DataInputStream(Files.newInputStream(summaries)).use(DataInputStream::readSummaries)
    }
}

class ModelArgs : PointGenerator<InputGenerationParameters, ModelParameters>() {
    @Parameter(names = arrayOf("--vm-allocation-time"), converter = RangeConverter::class,
               description = "Number of seconds for VM allocation")
    var vmAllocationTime: Range = Range.point(0.0)

    @Parameter(names = arrayOf("--num-resources"), converter = RangeConverter::class,
               description = "Maximal number of resources that can be allocated")
    var maxNumAllocatedResources: Range = Range.point(1.0)

    @Parameter(names = arrayOf("--allocation-cost"), converter = RangeConverter::class,
               description = "The cost of allocating a single VM")
    var allocationCost: Range = Range.point(0.0)

    @Parameter(names = arrayOf("--maintenance-cost"), converter = RangeConverter::class,
               description = "The cost (per second) of keeping a single VM running")
    var maintenanceCost: Range = Range.point(0.01)

    @Parameter(names = arrayOf("--buffer-size"), converter = RangeConverter::class,
               description = "Maximal number of buffered requests")
    var bufferSize: Range? = null

    init {
        addProp({vmAllocationTime}) { x, input, model -> model.copy(vmAllocationTime = input.secondsToTimeSlots(x))}
        addProp({maxNumAllocatedResources}) {x, _, model -> model.copy(maxNumAllocatedResources = x.toInt())}
        addProp({allocationCost}) { x, _, model -> model.copy(allocationCost = x)}
        addProp({maintenanceCost}) { x, input, model -> model.copy(maintenanceCost = input.perSecondToPerTimeSlot(x))}
        addProp({bufferSize}) { x, _, model -> model.copy(bufferSize = x.toInt())}
    }

    override fun initialParameters(value: InputGenerationParameters) = ModelParameters(
            vmAllocationTime = 0,
            maxNumAllocatedResources = 1,
            allocationCost = 0.0,
            maintenanceCost = 0.0,
            bufferSize = null
    )

    fun  toModelParameters(inputParams: InputGenerationParameters): ModelParameters =
            start(inputParams) { modelParams, _ -> modelParams }.single()
}

class PolicyArgs : PointGenerator<InputGenerationParameters, AuxiliaryPolicyParameters>() {
    @Parameter(names = arrayOf("--policy-param"), converter = RangeConverter::class,
               description = "Policy-specific paramter")
    var policyParam: Range? = null

    init {
        addProp({ policyParam }) { x, _, p -> p.copy(policyParam = x)}
    }

    override fun initialParameters(value: InputGenerationParameters): AuxiliaryPolicyParameters =
        AuxiliaryPolicyParameters(
                minimalDeadlineCushion = value.deadlines?.minimalCushion(),
                minimalValue = value.valueScale
        )

    fun toPolicyParameters(inputParams: InputGenerationParameters): AuxiliaryPolicyParameters =
        start(inputParams) { x, _ -> x }.single()
}

@Parameters
object CreateSummaryCommand {
    val name = "create-summaries"
    fun run(inputArgs: InputArgs) {
        val summaries = generateSummaries(System.`in`.bufferedReader().lineSequence().map { TaskEvent.Companion.fromCSVLine(it)})
        DataOutputStream(Files.newOutputStream(inputArgs.summaries)).use { it.writeSummaries(summaries.values) }
    }
}

@Parameters
object RunCommand {
    val name = "run"

    fun run(inputArgs: InputArgs, modelArgs: ModelArgs, policyArgs: PolicyArgs, policies: List<PolicyFactory>, optimalEstimatorType: OptimalEstimatorType) {
        val summaries = inputArgs.readSummaries()
        val inputParams = inputArgs.toInputGenerationParameters()
        val input = inputParams.generateInput(summaries)
        val modelParams = modelArgs.toModelParameters(input.parameters)
        val policyParams = policyArgs.toPolicyParameters(inputParams)

        for (policy in policies) {
            val infra = ConcreteInfrastructure(modelParams, optimalEstimatorType.getEstimator(modelParams, inputParams), policyParams, policy)
            infra.processAll(input.timeSlots)
            println("${policy.name}: " +
                    "objective = ${infra.accounting.objective} " +
                    "requests completed = ${infra.accounting.numRequestsCompleted}"
            )
        }
    }
}

@Parameters
object PrintStatsCommand {
    val name = "print-stats"

    fun run(inputArgs: InputArgs, modelArgs: ModelArgs) {
        val summaries = inputArgs.readSummaries()
        val inputParameters = inputArgs.toInputGenerationParameters()
        val input = inputParameters.generateInput(summaries)
        val modelParameters = modelArgs.toModelParameters(inputParameters)

        printInputStats(input, modelParameters)
    }
}


fun main(args: Array<String>) {
    val modelArgs = ModelArgs()
    val inputGenerationArgs = InputArgs()
    val genericArgs = GenericArgs()
    val policyArgs = PolicyArgs()

    val cmdLine = JCommander.newBuilder()
            .addObject(modelArgs)
            .addObject(inputGenerationArgs)
            .addObject(genericArgs)
            .addObject(policyArgs)
            .addCommand(PlotCommand.name, PlotCommand)
            .addCommand(CreateSummaryCommand.name, CreateSummaryCommand)
            .addCommand(RunCommand.name, RunCommand)
            .addCommand(PrintStatsCommand.name, PrintStatsCommand)
            .build()
    cmdLine.parse(*args)

    if (genericArgs.help) {
        cmdLine.usage()
        return
    }

    if (genericArgs.listPolicies) {
        println("Available policies: ${PolicyParseDescription.description}")
        return
    }

    val policies = try {
        genericArgs.getPolicyFactories()
    } catch (e: GenericArgs.UnknownPolicyException) {
        System.err.println("Unknown policy: ${e.policyName}")
        System.err.println("Available policies: ${PolicyParseDescription.description}")
        exitProcess(1)
    }

    val estimatorType = try {
        genericArgs.getEstimatorType()
    } catch (e: GenericArgs.UnknownOptimalEstimatorException) {
        System.err.println("Unknown estimator: ${e.estimator}")
        System.err.println("Available estimators: ${OptimalEstimatorType.values().map { it.name }.joinToString(", ")}")
        exitProcess(1)
    }

    when (cmdLine.parsedCommand) {
        PrintStatsCommand.name -> {
            PrintStatsCommand.run(inputGenerationArgs, modelArgs)
        }
        RunCommand.name -> {
            RunCommand.run(inputGenerationArgs, modelArgs, policyArgs, policies, estimatorType)
        }
        CreateSummaryCommand.name -> {
            CreateSummaryCommand.run(inputGenerationArgs)
        }
        PlotCommand.name -> {
            PlotCommand.run(inputGenerationArgs, modelArgs, policyArgs, policies, estimatorType)
        }
    }
}
