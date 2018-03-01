package hu.mktiti.kanon

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass

sealed class AttributeType<out T> {
    abstract fun parser(): (String) -> T
}

class AttributeParseException(message: String) : RuntimeException(message)

class IntAttribute(
        private val minValue: Int = Int.MIN_VALUE,
        private val maxValue: Int = Int.MAX_VALUE) : AttributeType<Int>() {

    override fun parser(): (String) -> Int = {
        val value = it.toInt()
        if (value in minValue..maxValue) value
        else throw AttributeParseException("Int value [$value] out of range [$minValue;$maxValue]")
    }

    override fun toString() =
            "Int attribute [${if (minValue == Int.MIN_VALUE) "" else minValue.toString()};${if (maxValue == Int.MAX_VALUE) "" else maxValue.toString()}]"
}

class HierarchicAttribute(
        private val valueSet: NamedRecursiveBlock) : AttributeType<String>() {

    override fun parser(): (String) -> String = {
        if (valueSet.contains(it)) it
        else throw AttributeParseException("Enum value [$it] not in set of possible values [$valueSet]")
    }

    override fun toString() = "${valueSet.name} (enum) attribute"
}

class EnumAttribute(
        private val valueSet: Set<String>) : AttributeType<String>() {

    override fun parser(): (String) -> String = {
        if (it in valueSet) it
        else throw AttributeParseException("Enum value [$it] not in set of possible values [$valueSet]")
    }

    override fun toString() = "One of $valueSet"
}

fun <T : Enum<*>> KClass<T>.asAttribute() = EnumAttribute(this.java.enumConstants.map { it.name }.toSet())

class StringAttribute(
        private val minLength: Int = 0,
        private val maxLength: Int = Int.MAX_VALUE) : AttributeType<String>() {

    override fun parser(): (String) -> String = {
        if (it.length in minLength..maxLength) it
        else throw AttributeParseException("String value [$it] out of length range [$minLength;$maxLength]")
    }

    override fun toString() =
            "String attribute (length: [$minLength;${if (maxLength == Int.MAX_VALUE) "" else maxLength.toString()}])"
}

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
}

data class Attribute<out T>(val name: String, val type: AttributeType<T>, val quasiIdentifier: Boolean, val secret: Boolean) {

    override fun toString() = "{$name: $type}"
}

data class RecordDescriptor(private val attributes: List<Attribute<*>>) {
    constructor(vararg attributes: Attribute<*>) : this(attributes.toList())
}