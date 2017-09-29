package utils

import model.Request
import model.ModelParameters
import policies.coldUnitValue
import policies.hotUnitValue
import traces.DeadlineParameters
import traces.Input
import traces.InputGenerationParameters

fun optional(condition: Boolean, str: () -> String) =
   if (condition) str() else ""

data class SequenceStats(val min: Double, val max: Double, val mean: Double, val sd: Double, val median: Double)

fun  sequenceStats(input: Iterable<Double>): SequenceStats {
    val sorted = input.sorted()
    val mean = sorted.sum() / sorted.size
    val sd = Math.sqrt(sorted.map { (it - mean) * (it - mean) }.sum() / sorted.size)
    return SequenceStats(min = input.min()!!, max = input.max()!!, mean = mean, sd = sd, median = sorted[sorted.size / 2])
}

fun printInputStats(input: Input, modelParameters: ModelParameters) {
    val requestsPerTimeSlot = input.timeSlots.map { it.size }.filter { it != 0 }.map(Int::toDouble)
    val processingTimes = input.timeSlots.flatMap {
        it.map{ it.initialProcessingTime * input.parameters.timeSlotDuration / (InputGenerationParameters.ONE_SECOND * 60).toDouble() }
    }
    val values = input.timeSlots.flatMap{ it.map(Request::value) }
    val deadlineCushions = if (input.parameters.deadlines == null) null else
        input.timeSlots.flatMap { it.map { it.deadline!!.toDouble() / it.initialProcessingTime }}
    val huvs = input.timeSlots.flatMap { it.map{ hotUnitValue(it, modelParameters) }}
    val cuvs = input.timeSlots.flatMap { it.map{ coldUnitValue(it, modelParameters) }}

    println("Parameters: ${input.parameters}" )
    println("Total number of requests: ${requestsPerTimeSlot.sum().toInt()}")
    println("Duration: ${input.timeSlots.size * input.parameters.timeSlotDuration / (InputGenerationParameters.ONE_SECOND * 60).toDouble()} minutes")
    println("Number of time slots: ${input.timeSlots.size}")
    println("Values:  ${sequenceStats(values)}")
    println("Requests per time slot: ${sequenceStats(requestsPerTimeSlot)}")
    println("Processing times (minutes): ${sequenceStats(processingTimes)}")
    if (deadlineCushions != null) {
        println("Deadline cushions: ${sequenceStats(deadlineCushions)}")
    }
    println("Hot unit values: ${sequenceStats(huvs)}")
    println("Cold unit values: ${sequenceStats(cuvs)}")
}

fun InputGenerationParameters.secondsToTimeSlots(duration: Double): Int =
        Math.ceil((duration * InputGenerationParameters.ONE_SECOND) / timeSlotDuration).toInt()

fun InputGenerationParameters.timeSlotsToSeconds(timeSlots: Double): Double =
        timeSlots * timeSlotDuration / InputGenerationParameters.ONE_SECOND

fun InputGenerationParameters.perSecondToPerTimeSlot(value: Double): Double =
        (value / InputGenerationParameters.ONE_SECOND) * timeSlotDuration

fun InputGenerationParameters.perTimeSlotToPerSecond(value: Double): Double =
        value / timeSlotDuration * InputGenerationParameters.ONE_SECOND

fun DeadlineParameters.minimalCushion(): Double =
        schedulingClassCushions.min()!! * cushionsScale