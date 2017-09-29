package policies

import policies.aap.AAPParseDescription
import policies.learn.LearnParseDescription
import policies.nrap.NRAPParseDescription
import policies.pppq.PPPQParseDescription
import policies.pq.PQParseDescription
import java.util.regex.Matcher
import java.util.regex.Pattern

interface PolicyParseDescription {
    companion object {
        private val allDescriptions = listOf(NRAPParseDescription, PQParseDescription, PPPQParseDescription, AAPParseDescription, LearnParseDescription)

        private val allPatterns = allDescriptions.flatMap { it.patterns }
        fun createFactory(policyName: String): PolicyFactory? =
                allPatterns.map {
                    val matcher = it.pattern.matcher(policyName)
                    if (matcher.find()) { it.instantiatePolicy(matcher) } else { null }
                }.filterNotNull().singleOrNull()
        val description: String = allDescriptions.map(PolicyParseDescription::description).joinToString(", ")
    }

    abstract class PolicyPattern(val pattern: Pattern) {
        abstract fun instantiatePolicy(matcher: Matcher): PolicyFactory?
    }

    val description: String
    val patterns: Collection<PolicyPattern>
}