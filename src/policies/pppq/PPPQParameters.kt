package policies.pppq

data class PPPQParameters(val pessimisticFactor: Double = 0.0, val preemptionFactor: Double = 1.0) {
    companion object {
        fun fromDeadlineCushion(deadlineCushion: Double) =
                findClosestParametersEntry(deadlineCushion).toPPPQParameters()

        private var entries: List<ParametersEntry> =
            ClassLoader.getSystemClassLoader().getResourceAsStream("opt_params.tsv")
            .bufferedReader().useLines { lines ->
                lines.map {
                    val entry = it.split('\t').map(String::toDouble)
                    ParametersEntry(entry[0], entry[1], entry[2], entry[3])
                }.toList()
            }

        private fun findClosestParametersEntry(deadlineCushion: Double): ParametersEntry {
            return findClosest(1 + deadlineCushion, ParametersEntry::absoluteCushion, entries)
        }

        private fun <U> findClosest(value: Double, valueSelector: (U) -> Double, entries: List<U>): U {
            val closestIdx = entries.binarySearchBy(value, selector = valueSelector)
            val upperBound = if (closestIdx >= 0) closestIdx else -closestIdx - 1
            if (upperBound == 0 || upperBound < entries.size &&
                    Math.abs(valueSelector(entries[upperBound]) - value) < Math.abs(valueSelector(entries[upperBound - 1]) - value)) {
                return entries[upperBound]
            } else {
                return entries[upperBound - 1]
            }
        }

        private data class ParametersEntry(val absoluteCushion: Double, val competitiveness: Double, val preemptionFactor: Double, val pessimisticFactor: Double) {
            fun toPPPQParameters() = PPPQParameters(pessimisticFactor, preemptionFactor)
        }
    }
}