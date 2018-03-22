package hu.mktiti.kanon.attribute

import kotlin.math.max

/**
 * Attribute to represent strings
 *
 * @property hiddenChar character to use for stubbing representation
 * @property minLength smallest possible length for value of column
 * @property maxLength largest possible length for value of column
 */
class StringAttribute(
        private val hiddenChar: Char = '*',
        private val minLength: Int = 0,
        private val maxLength: Int = Int.MAX_VALUE) : AttributeType<StringAttributeValue>() {

    override fun parse(string: String): StringAttributeValue {
        return if (string.length in minLength..maxLength) {
            val hidden = string.reversed().takeWhile { it == hiddenChar }.length
            if (hidden == 0) {
                SimpleStringValue(string)
            } else {
                MaskedValue(string, hidden, hiddenChar)
            }
        } else {
            throw AttributeParseException("String value [$string] out of length range [$minLength;$maxLength]")
        }
    }

    override fun toString() =
            "String attribute (length: [$minLength;${if (maxLength == Int.MAX_VALUE) "" else maxLength.toString()}])"

    override fun show(value: StringAttributeValue) = value.toString()

    override fun subsetOf(parent: StringAttributeValue, child: StringAttributeValue) = child in parent

}

/**
 * String value type
 */
sealed class StringAttributeValue : AttributeValue {
    abstract operator fun contains(child: StringAttributeValue ): Boolean

    abstract operator fun contains(value: String): Boolean
}

/**
 * Simple string value
 */
class SimpleStringValue(val value: String) : StringAttributeValue() {
    override fun contains(child: StringAttributeValue) = when (child) {
        is SimpleStringValue -> value == child.value
        is MaskedValue       -> value == child.value && child.simpleValue
    }

    override fun contains(value: String) = value == this.value

    override fun toString() = value
}

/**
 * Stubbed string value
 */
class MaskedValue(val value: String, val hiddenChars: Int, val hiddenChar: Char) : StringAttributeValue() {

    val simpleValue: Boolean
        get() = hiddenChars == 0

    override operator fun contains(child: StringAttributeValue) = when (child) {
        is SimpleStringValue -> child.value in this
        is MaskedValue       -> {
            val strictHidden = max(hiddenChars, child.hiddenChars)
            value.dropLast(strictHidden) == this.value.dropLast(strictHidden)
        }
    }

    override operator fun contains(value: String) = value.dropLast(hiddenChars) == this.value.dropLast(hiddenChars)

    override fun toString() = value.dropLast(hiddenChars) + (hiddenChar.toString().repeat(hiddenChars))

}