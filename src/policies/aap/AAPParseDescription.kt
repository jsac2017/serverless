package policies.aap

import policies.PolicyFactory
import policies.PolicyParseDescription
import policies.PolicyParseDescription.PolicyPattern
import java.util.regex.Matcher
import java.util.regex.Pattern

object AAPParseDescription : PolicyParseDescription {
    override val description: String = "AAP(rho)"
    override val patterns = listOf<PolicyPattern>(
            object : PolicyPattern(Pattern.compile("AAP\\((?<rho>([0-9.]+|var))\\)")) {
                override fun instantiatePolicy(matcher: Matcher): PolicyFactory =
                    if (matcher.group("rho") == "var") {
                        AAPPolicyVarFactory()
                    } else {
                        AAPPolicyFactory(matcher.group("rho").toDouble())
                    }
            }
    )
}
