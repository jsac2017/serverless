package policies.learn

import policies.PolicyFactory
import policies.PolicyParseDescription
import policies.PolicyParseDescription.PolicyPattern
import policies.pq.PQParseDescription
import java.util.regex.Matcher
import java.util.regex.Pattern

object LearnParseDescription : PolicyParseDescription {
    override val description: String = "Learn({median,average}:time)"
    override val patterns: Collection<PolicyPattern> = listOf<PolicyPattern>(
            object : PolicyPattern(Pattern.compile("Learn\\((?<cmp>.+):(?<method>[a-z]+):(?<time>[0-9.]+)\\)")) {
                override fun instantiatePolicy(matcher: Matcher): PolicyFactory? {
                    val comparator = PQParseDescription.COMPARATORS[matcher.group("cmp")] ?: return null
                    val method = try {
                        LearnAdaptorPolicy.LearningMethod.valueOf(matcher.group("method").toUpperCase())
                    } catch (e: IllegalArgumentException) {
                        return null
                    }
                    return LearningPolicyFactory(matcher.group("cmp"), comparator, method, matcher.group("time").toInt())
                }
            }
    )
}