package hu.mktiti.kanon.attribute

class IntAttribute(
        private val minValue: Int = Int.MIN_VALUE,
        private val maxValue: Int = Int.MAX_VALUE) : AttributeType<IntAttributeValue>() {

    private fun safeInRange(value: Int): Int
            = if (value in minValue..maxValue) value else throw AttributeParseException("Int value [$value] out of range [$minValue;$maxValue]")

    override fun parse(string: String): IntAttributeValue {
        val cleaned = string.trim()

        val single = cleaned.toIntOrNull()
        if (single != null) {
            return SimpleIntValue(safeInRange(single))
        }

        try {
            val tokens = cleaned.split(':').map(String::toInt)
            if (tokens.size == 2) {
                return IntRangeValue(safeInRange(tokens[0]), safeInRange(tokens[1]))
            } else {
                throw AttributeParseException("Cannot parse int attribute '$string', invalid range")
            }
        } catch (nfe: NumberFormatException) {
            throw AttributeParseException("Cannot parse int attribute '$string'")
        }
    }

    override fun toString() =
            "Int attribute [${if (minValue == Int.MIN_VALUE) "" else minValue.toString()};${if (maxValue == Int.MAX_VALUE) "" else maxValue.toString()}]"

    override fun show(value: IntAttributeValue) = when (value) {
        is SimpleIntValue -> value.value.toString()
        is IntRangeValue  -> "${value.start} : ${value.end}"
    }

    override fun subsetOf(parent: IntAttributeValue, child: IntAttributeValue) = child in parent

}

sealed class IntAttributeValue : AttributeValue {
    abstract operator fun contains(child: IntAttributeValue): Boolean

    abstract operator fun contains(value: Int): Boolean
}

class SimpleIntValue(val value: Int) : IntAttributeValue() {
    override fun contains(child: IntAttributeValue) = when (child) {
        is SimpleIntValue -> value == child.value
        is IntRangeValue  -> child.start == value && child.simpleValue
    }

    override fun contains(value: Int) = value == this.value
}

class IntRangeValue(val start: Int, val end: Int) : IntAttributeValue() {

    val simpleValue: Boolean
        get() = start == end

    override operator fun contains(child: IntAttributeValue) = when (child) {
        is SimpleIntValue -> child.value in this
        is IntRangeValue  -> child.start in this && child.end in this
    }

    override operator fun contains(value: Int) = value in start..end

}