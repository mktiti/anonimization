package hu.mktiti.kanon.attribute

/**
 * Attribute to represent integers
 *
 * @property minValue smallest possible value for column
 * @property maxValue largest possible value for column
 */
class IntAttribute(
        minValue: Int = Int.MIN_VALUE,
        maxValue: Int = Int.MAX_VALUE) : RangeAttribute<Int, IntAttributeValue>(minValue, maxValue) {

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

    override fun simpleValue(value: Int) = SimpleIntValue(value)

    override fun rangeValue(min: Int, max: Int) = IntRangeValue(min, max)
}

/**
 * Integer value type
 */
sealed class IntAttributeValue : RangeAttributeValue<Int> {
    override fun rangeSize(): Long = max() - min() + 1L
}

/**
 * Simple integer value containing simple number
 */
class SimpleIntValue(val value: Int) : IntAttributeValue() {
    override fun min() = value

    override fun max() = value
}

/**
 * Range of inter values
 */
class IntRangeValue(val start: Int, val end: Int) : IntAttributeValue() {
    override fun min() = start

    override fun max() = end
}