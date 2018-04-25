package hu.mktiti.kanon.attribute

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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
        after: LocalDate = LocalDate.MIN,
        before: LocalDate = LocalDate.MAX) : RangeAttribute<LocalDate, DateAttributeValue>(after, before) {

    private val formatter = DateTimeFormatter.ofPattern(formatterString)

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
            "Date attribute [${if (minValue == LocalDate.MIN) "" else minValue.toString()};${if (maxValue == LocalDate.MAX) "" else maxValue.toString()}]"

    override fun simpleValue(value: LocalDate) = SimpleDateValue(value)

    override fun rangeValue(min: LocalDate, max: LocalDate) = DateRangeValue(min, max)

}

/**
 * Date value type
 */
sealed class DateAttributeValue : AttributeValue, RangeAttributeValue<LocalDate> {
    override fun rangeSize(): Long = ChronoUnit.DAYS.between(max(), min()) + 1
}

/**
 * Simple date value containing simple date
 */
class SimpleDateValue(val value: LocalDate) : DateAttributeValue() {
    override fun min() = value

    override fun max() = value

}

/**
 * Date range used for data stubbing
 */
class DateRangeValue(val min: LocalDate, val max: LocalDate) : DateAttributeValue() {
    override fun min() = min

    override fun max() = max

}