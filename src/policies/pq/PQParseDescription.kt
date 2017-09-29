package policies.pq

import model.MutableRequest
import policies.PolicyFactory
import policies.PolicyParseDescription
import policies.PolicyParseDescription.PolicyPattern
import java.util.regex.Matcher
import java.util.regex.Pattern

object PQParseDescription : PolicyParseDescription {
    val COMPARATORS = mapOf<String, Comparator<MutableRequest>>(
            "v/w" to compareByDescending { it.value / it.remainingProcessingTime },
            "v/d" to compareByDescending { it.value / (it.currentDeadline!! + 1) },
            "v/wi" to compareByDescending { it.value / it.initialProcessingTime },
            "-d" to compareBy { it.currentDeadline!! },
            "v" to compareByDescending { it.value },
            "-w" to compareBy { it.remainingProcessingTime }
    )

    override val description: String = "PQ([${PQParseDescription.COMPARATORS.keys.joinToString("|")}])"
    override val patterns = listOf<PolicyPattern>(
            object : PolicyPattern(Pattern.compile("PQ\\((?<id>.*)\\)")) {
                override fun instantiatePolicy(matcher: Matcher): PolicyFactory? =
                        PQParseDescription.COMPARATORS[matcher.group("id")]?.let {
                            PQPolicyFactory(matcher.group("id"), it)
                        }
            }
    )
}