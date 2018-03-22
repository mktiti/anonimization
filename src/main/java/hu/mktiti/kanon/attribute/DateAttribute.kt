package hu.mktiti.kanon.attribute

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Attribute to represent dates
 *
 * @param formatterString input date format
 *
 * @property rangeSeparator input range separator
 * @property after smallest possible value for column
 * @property before largest possible value for column
 */
class DateAttribute(
        formatterString: String = "yyyy-MM-dd",
        private val rangeSeparator: Char = ':',
        private val after: LocalDate = LocalDate.MIN,
        private val before: LocalDate = LocalDate.MAX) : AttributeType<DateAttributeValue>() {

    private val formatter = DateTimeFormatter.ofPattern(formatterString)

    private fun safeInRange(value: LocalDate): LocalDate
            = if (value in after..before) value else throw AttributeParseException("Date value [$value] out of range [$after;$before]")

    override fun parse(string: String): DateAttributeValue {
        val cleaned = string.trim()

        return if (cleaned.contains(rangeSeparator)) {
            val tokens = cleaned.split(rangeSeparator).map { LocalDate.parse(it, formatter) }
            if (tokens.size == 2) {
                DateRangeValue(safeInRange(tokens[0]), safeInRange(tokens[1]))
            } else {
                throw AttributeParseException("Cannot parse date attribute '$string', invalid range")
            }
        } else {
            SimpleDateValue(safeInRange(LocalDate.parse(cleaned, formatter)))
        }
    }

    override fun show(value: DateAttributeValue) = when (value) {
        is SimpleDateValue -> value.value.toString()
        is DateRangeValue  -> "${value.min} : ${value.max}"
    }

    override fun toString() =
            "Date attribute [${if (after == LocalDate.MIN) "" else after.toString()};${if (before == LocalDate.MAX) "" else before.toString()}]"

    override fun subsetOf(parent: DateAttributeValue, child: DateAttributeValue) = child in parent
}

/**
 * Date value type
 */
sealed class DateAttributeValue : AttributeValue {
    abstract operator fun contains(child: DateAttributeValue): Boolean

    abstract operator fun contains(value: LocalDate): Boolean
}

/**
 * Simple date value containing simple date
 */
class SimpleDateValue(val value: LocalDate) : DateAttributeValue() {
    override fun contains(child: DateAttributeValue) = when (child) {
        is SimpleDateValue -> value == child.value
        is DateRangeValue  -> child.min == value && child.simpleValue
    }

    override fun contains(value: LocalDate) = value == this.value
}

/**
 * Date range used for data stubbing
 */
class DateRangeValue(val min: LocalDate, val max: LocalDate) : DateAttributeValue() {

    val simpleValue: Boolean
        get() = min == max

    override operator fun contains(child: DateAttributeValue) = when (child) {
        is SimpleDateValue -> child.value in this
        is DateRangeValue  -> child.min in this && child.max in this
    }

    override operator fun contains(value: LocalDate) = value in min..max

}