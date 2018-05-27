package hu.mktiti.kanon.attribute

import kotlin.reflect.KClass

fun <T : Enum<*>> KClass<T>.asAttribute() = EnumAttribute(this.java.enumConstants.map { it.name }.toSet())

class EnumAttribute(
        private val valueSet: Set<String>) : QuasiAttributeType<EnumAttributeValue>() {

    constructor(values: Collection<String>) : this(values.toSet())

    constructor(vararg values: String) : this(values.toSet())

    override fun parse(string: String): EnumAttributeValue {
        val cleaned = string.trim()
        return if (cleaned in valueSet) SimpleEnumValue(cleaned)
               else throw AttributeParseException("Value '$cleaned' not in enum value set ${valueSet.joinToString(prefix = "{", postfix = "}")}")
    }

    fun simplify(attributeValue: EnumAttributeValue): EnumAttributeValue
        = if (attributeValue is ListEnumValue && attributeValue.asList().size == 1) SimpleEnumValue(attributeValue.asList().first()) else attributeValue

    private fun toAttributeValue(values: List<String>) = when (values.size) {
        0 -> ListEnumValue(listOf())
        1 -> SimpleEnumValue(values[0])
        else -> ListEnumValue(values)
    }

    override fun subsetOf(parent: EnumAttributeValue, child: EnumAttributeValue) = (parent.asList() - child.asList()).isEmpty()

    override fun smallestGeneralization(values: List<EnumAttributeValue>) = toAttributeValue(values.flatMap(EnumAttributeValue::asList).distinct())

    override fun partitionError(partition: Partition<EnumAttributeValue>): Double {
        val partitionSize = partition.aggregateValue.asList().size
        return partition.values.map { partitionSize / it.asList().size.toDouble() }.average() // Mean error
    }

    override fun splitToParts(partition: Partition<EnumAttributeValue>, kValue: Int): Pair<List<EnumAttributeValue>, List<EnumAttributeValue>>? {
        if (partition.values.size < kValue * 2) return null
        assert(kValue >= 1)

        val growing: MutableList<EnumAttributeValue> = ArrayList(partition.values.size)
        val shrinking: MutableList<EnumAttributeValue> = ArrayList(partition.values)

        while (growing.size < kValue && shrinking.size > kValue) {
            val newlyAdded = mutableSetOf<String>()

            val lead = shrinking.maxBy { it.asList().size } ?: return null
            shrinking.remove(lead)
            newlyAdded.addAll(lead.asList())
            growing.add(lead)

            var oldSize: Int
            do {
                oldSize = newlyAdded.size

                val toAdd = shrinking.filter { (it.asList() intersect newlyAdded).isNotEmpty() }
                shrinking.removeAll(toAdd)
                newlyAdded.addAll(toAdd.flatMap(EnumAttributeValue::asList))
                growing.addAll(toAdd)
            } while (oldSize != newlyAdded.size)
        }

        if (growing.size < kValue || shrinking.size < kValue) return null

        return growing to shrinking
    }
}

sealed class EnumAttributeValue : AttributeValue {
    abstract fun contains(value: String): Boolean

    abstract fun asList(): List<String>
}

class SimpleEnumValue(val value: String) : EnumAttributeValue() {
    override fun contains(value: String) = value == this.value

    override fun asList() = listOf(value)

    override fun toString() = value
}

class ListEnumValue(val values: List<String>) : EnumAttributeValue() {
    override fun contains(value: String) = values.contains(value)

    override fun asList() = values

    override fun toString() = values.joinToString(prefix = "[", separator = ", ", postfix = "]")
}