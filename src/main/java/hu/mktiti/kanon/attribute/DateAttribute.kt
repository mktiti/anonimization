package hu.mktiti.kanon.attribute

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DateAttribute(
        formatterString: String = "yyyy-MM-dd",
        private val after: LocalDate = LocalDate.MIN,
        private val before: LocalDate = LocalDate.MAX) : AttributeType<LocalDate>() {

    private val formatter = DateTimeFormatter.ofPattern(formatterString)

    override fun parser(): (String) -> LocalDate = {
        val value = LocalDate.parse(it, formatter)
        if (value in after..before) value
        else throw AttributeParseException("Date value [$value] out of range [$after;$before]")
    }

    override fun toString() =
            "Date attribute [${if (after == LocalDate.MIN) "" else after.toString()};${if (before == LocalDate.MAX) "" else before.toString()}]"

    override fun isSame(a: LocalDate, b: LocalDate) = a == b
}