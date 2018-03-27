package hu.mktiti.kanon.attribute

import hu.mktiti.kanon.NamedRecursiveBlock
import java.util.*

/**
 * Attribute to represent (structured) enums
 *
 * @property valueSet root of accepted Enum values
 */
class HierarchicAttribute(
        private val valueSet: NamedRecursiveBlock) : AttributeType<HierarchicAttributeValue>() {

    override fun parse(string: String): HierarchicAttributeValue {
        return valueSet.find(string)?.let(::SimpleHierarchicValue) ?:
        throw AttributeParseException("Enum value [$string] not in set of possible values [$valueSet]")
    }

    override fun toString() = "${valueSet.name} (enum) attribute"

    override fun show(value: HierarchicAttributeValue): String =
            searchPath((value as SimpleHierarchicValue).value.name)?.joinToString(separator = ".", transform = NamedRecursiveBlock::name)

    private fun searchPath(value: String, root: NamedRecursiveBlock = valueSet): List<NamedRecursiveBlock> {
        if (value == root.name) return listOf(root)

        for (child in root.content) {
            val result = searchPath(value, child)
            if (result.isNotEmpty()) {
                return LinkedList(result).apply { addFirst(root) }
            }
        }

        return emptyList()
    }

    override fun subsetOf(parent: HierarchicAttributeValue, child: HierarchicAttributeValue) = child in parent

    override fun smallestGeneralization(values: List<HierarchicAttributeValue>): HierarchicAttributeValue {
        if (values.isEmpty()) return SimpleHierarchicValue(valueSet)
        if (values.size == 1) return values[0]

        return when (values.size) {
            0 -> SimpleHierarchicValue(valueSet)
            1 -> values[0]
            else -> values.reduce { a, b ->
                val aFull = searchPath(a.value.name, valueSet)
                val bFull = searchPath(b.value.name, valueSet)

                for (i in 0 until minOf(aFull.size, bFull.size)) {
                    if (aFull[i] != bFull[i]) {
                        return SimpleHierarchicValue(aFull[i - 1])
                    }
                }

                SimpleHierarchicValue(listOf(aFull, bFull).minBy { it.size }!!.last())
            }
        }

    }
}

/**
 * Enum value type
 */
sealed class HierarchicAttributeValue(val value: NamedRecursiveBlock) : AttributeValue {
    abstract operator fun contains(child: HierarchicAttributeValue): Boolean
}

/**
 * Enum value
 */
class SimpleHierarchicValue(value: NamedRecursiveBlock) : HierarchicAttributeValue(value) {
    override operator fun contains(child: HierarchicAttributeValue) = value.contains((child as SimpleHierarchicValue).value.name)
}