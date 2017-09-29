package main.cli

import com.beust.jcommander.IStringConverter
import utils.Range

class RangeConverter : IStringConverter<Range> {
    override fun convert(str: String): Range {
        val values = str.split(":")
        if (values.size == 3) {
            return Range.linear(start = values[0].toDouble(), end = values[1].toDouble(), numSegments = values[2].toInt())
        } else if (values.size == 4 && values[3] == "exp") {
            return Range.exponential(start = values[0].toDouble(), end = values[1].toDouble(), numSegments = values[2].toInt())
        } else if (values.size == 1) {
            return Range.point(values[0].toDouble())
        } else {
            throw IllegalArgumentException("Unknown representation of the range: $str")
        }
    }
}

