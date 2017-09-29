package policies.nrap

import policies.PolicyFactory
import policies.PolicyParseDescription
import policies.PolicyParseDescription.PolicyPattern
import java.util.regex.Matcher
import java.util.regex.Pattern

object NRAPParseDescription : PolicyParseDescription {
    override val patterns = listOf<PolicyPattern>(
            object : PolicyPattern(Pattern.compile("NRAP")) {
                override fun instantiatePolicy(matcher: Matcher): PolicyFactory =
                        NRAPPolicyFactory()
            }
    )
    override val description: String = "NRAP"
}