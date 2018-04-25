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

    override fun smallestGeneralization(values: List<StringAttributeValue>): StringAttributeValue {
        if (values.isEmpty()) return SimpleStringValue("")

        val longest = values.maxBy { it.value.length }!!
        if (values.map { it.value.length }.distinct().size > 1) {
            return MaskedValue(longest.value, longest.value.length, hiddenChar)
        }

        for (i in 0 until longest.value.length) {
            if (values.map { it.toString()[i] }.filter { it != hiddenChar }.distinct().size > 1) {
                return MaskedValue(longest.value, longest.value.length - i, hiddenChar)
            }
        }

        return longest
    }

    override fun split(partition: Partition<StringAttributeValue>, kValue: Int): PartitionSplit<StringAttributeValue>? {
        return null
    }

}

/**
 * String value type
 */
sealed class StringAttributeValue(val value: String) : AttributeValue {
    abstract operator fun contains(child: StringAttributeValue ): Boolean

    abstract operator fun contains(value: String): Boolean

    abstract fun length(): Int
}

/**
 * Simple string value
 */
class SimpleStringValue(value: String) : StringAttributeValue(value) {
    override fun contains(child: StringAttributeValue) = when (child) {
        is SimpleStringValue -> value == child.value
        is MaskedValue       -> value == child.value && child.simpleValue
    }

    override fun contains(value: String) = value == this.value

    override fun toString() = value

    override fun length() = value.length
}

/**
 * Stubbed string value
 */
class MaskedValue(value: String, private val hiddenChars: Int, private val hiddenChar: Char) : StringAttributeValue(value) {

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

    override fun length() = max(value.length, hiddenChars)
}