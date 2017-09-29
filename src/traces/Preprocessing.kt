package traces

import model.Request
import utils.optional
import java.text.DecimalFormat
import java.util.*

private val FORMAT = DecimalFormat("0.###")

data class DeadlineParameters(
        val schedulingClassCushions: List<Double>,
        val cushionsScale: Double,
        val isRandomized: Boolean
) {
    override fun toString(): String =
        "[" + schedulingClassCushions.map { FORMAT.format(it) }.joinToString(",") + "]" +
                "*${FORMAT.format(cushionsScale)}"

}

fun DeadlineParameters.effectiveSchedulingCushion(schedulingClass: Int, rng: Random): Double {
    val lower = schedulingClassCushions[schedulingClass] * cushionsScale
    if (isRandomized) {
        return rng.nextDouble() * lower + lower
    } else {
        return lower
    }
}

data class InputGenerationParameters(
        val timeSlotDuration: Long,
        val processingScale: Double,
        val unitProcessing: Boolean,
        val valueScale: Double,
        val valueExpBase: Double?,
        val unitValue: Boolean,
        val deadlines: DeadlineParameters?
) {
    companion object {
        val ONE_SECOND = 1_000_000L
    }

    override fun toString(): String =
        "(" +
            "dT=${timeSlotDuration / ONE_SECOND}s" +
            ",vscale=${FORMAT.format(valueScale)}" +
            ",wscale=${FORMAT.format(processingScale)}" +
            ",cs=$deadlines" +
            optional(unitProcessing) {",uw"} +
            optional(valueExpBase != null) {",vexp=${FORMAT.format(valueExpBase!!)}"} +
        ")"
}


data class Input(val parameters: InputGenerationParameters, val timeSlots: List<List<Request>>)

data class TaskEventsSummary(
        val priority: Int,
        val schedulingClass: Int,
        val firstSubmissionTime: Long,
        var lastScheduleTime: Long? = null,
        var finishTime: Long? = null,
        var wasResubmitted: Boolean = false
)

fun generateSummaries(taskEvents: Sequence<TaskEvent>): Map<TaskID, TaskEventsSummary> {
    val eventSummaries = HashMap<TaskID, TaskEventsSummary>()

    taskEvents.forEach {
        when (it.eventType) {
            EventType.SUBMIT ->
                if (!eventSummaries.contains(it.id)) {
                    eventSummaries.put(it.id, TaskEventsSummary(it.priority, it.schedulingClass, it.timestamp))
                } else {
                    eventSummaries[it.id]?.wasResubmitted = true
                }
            EventType.FINISH -> eventSummaries[it.id]?.finishTime = it.timestamp
            EventType.SCHEDULE -> eventSummaries[it.id]?.lastScheduleTime = it.timestamp

            else -> Unit
        }
    }

    println("Total number of tasks: ${eventSummaries.size}")
    val startedDuringMonitor = eventSummaries.filterValues { it.firstSubmissionTime > 0 }
    println("Number of tasks started after 0: ${startedDuringMonitor.size}")
    // Surprisingly, there are finished jobs that were never scheduled!!!
    val finished = startedDuringMonitor.filterValues { it.lastScheduleTime != null && it.finishTime != null }
    println("Number of those that have finished: ${finished.size}")
    val wasNotResubmitted = finished.filterValues { !it.wasResubmitted }
    println("Number of those that were not resubmitted: ${wasNotResubmitted.size}")

    return wasNotResubmitted
}

fun InputGenerationParameters.generateInput(summaries: Collection<TaskEventsSummary>, rng: Random = Random()): Input {
    val result = mutableListOf<List<Request>>()

    var currentTimeSlotStart: Long = summaries.map { it.firstSubmissionTime }.min()!!
    var currentTimeSlot = mutableListOf<Request>()
    var lastID = 0

    summaries.sortedBy { it.firstSubmissionTime }.forEach {
        while (it.firstSubmissionTime >= currentTimeSlotStart + timeSlotDuration) {
            result.add(currentTimeSlot)
            currentTimeSlot = mutableListOf()
            currentTimeSlotStart += timeSlotDuration
        }

        val releaseTime = result.size
        val processingTime = if (unitProcessing) 1.0 else
            Math.ceil((it.finishTime!! - it.lastScheduleTime!!) * processingScale / timeSlotDuration.toDouble())
        val value = if (unitValue) 1.0 else
            valueScale * if (valueExpBase == null)
                (it.priority + 1.0) else Math.pow(valueExpBase, it.priority.toDouble())
        val deadline = if (deadlines == null) null else
            Math.ceil(processingTime * (1 + deadlines.effectiveSchedulingCushion(it.schedulingClass, rng))).toInt()

        currentTimeSlot.add(Request(
                id = lastID++,
                releaseTime = releaseTime,
                deadline = deadline,
                value = value,
                initialProcessingTime = processingTime.toInt()
        ))
    }
    if (!currentTimeSlot.isEmpty()) {
        result.add(currentTimeSlot)
    }

    return Input(this, result)
}

