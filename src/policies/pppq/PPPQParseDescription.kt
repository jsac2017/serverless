package policies.pppq

import policies.PolicyParseDescription
import policies.PolicyParseDescription.PolicyPattern
import java.util.regex.Matcher
import java.util.regex.Pattern

object PPPQParseDescription : PolicyParseDescription {
    override val description: String = "PPPQ({b:y,c,opt,var}[:wc])"

    override val patterns = listOf(
        object : PolicyPattern(Pattern.compile("PPPQ\\((?<pessimistic>[0-9.]+):(?<preemption>[0-9.]+)(?<wc>(:wc)?)\\)")) {
            override fun instantiatePolicy(matcher: Matcher): PPPQPolicyFactory =
                PPPQPolicyFactory.fromParameters(
                    pessimisticFactor = matcher.group("pessimistic").toDouble(),
                    preemptionFactor = matcher.group("preemption").toDouble(),
                    workConservative = matcher.group("wc").isNotEmpty()
                )
        },
        object : PolicyPattern(Pattern.compile("PPPQ\\((?<c>[0-9.]+)(?<wc>(:wc)?)\\)")) {
            override fun instantiatePolicy(matcher: Matcher): PPPQPolicyFactory =
                PPPQPolicyFactory.fromDeadlineCushion(
                    deadlineCushion = matcher.group("c").toDouble(),
                    workConservative = matcher.group("wc").isNotEmpty()
                )
        },
        object : PolicyPattern(Pattern.compile("PPPQ\\(opt(?<wc>(:wc)?)\\)")) {
            override fun instantiatePolicy(matcher: Matcher): PPPQPolicyFactory =
                PPPQPolicyFactory.inputOptimalCushion(matcher.group("wc").isNotEmpty())
        },
        object : PolicyPattern(Pattern.compile("PPPQ\\(var(?<wc>(:wc)?)\\)")) {
            override fun instantiatePolicy(matcher: Matcher): PPPQPolicyFactory =
                PPPQPolicyFactory.variableCushion(matcher.group("wc").isNotEmpty())
        }
    )
}