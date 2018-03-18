package hu.mktiti.kanon.attribute

import hu.mktiti.kanon.NamedRecursiveBlock
import java.util.*

class HierarchicAttribute(
        private val valueSet: NamedRecursiveBlock) : AttributeType<HierarchicAttributeValue>() {

    override fun parse(string: String): HierarchicAttributeValue {
        return valueSet.find(string)?.let(::SimpleHierarchicValue) ?:
        throw AttributeParseException("Enum value [$string] not in set of possible values [$valueSet]")
    }

    override fun toString() = "${valueSet.name} (enum) attribute"

    override fun show(value: HierarchicAttributeValue): String =
            searchPath((value as SimpleHierarchicValue).value.name)?.joinToString(separator = ".", transform = NamedRecursiveBlock::name) ?: "---"

    private fun searchPath(value: String, root: NamedRecursiveBlock = valueSet): List<NamedRecursiveBlock>? {
        if (value == root.name) return listOf(root)

        for (child in root.content) {
            val result = searchPath(value, child)
            if (result != null) {
                return LinkedList(result).apply { addFirst(root) }
            }
        }

        return null
    }

    override fun subsetOf(parent: HierarchicAttributeValue, child: HierarchicAttributeValue) = child in parent
}

sealed class HierarchicAttributeValue : AttributeValue {
    abstract operator fun contains(child: HierarchicAttributeValue): Boolean
}

class SimpleHierarchicValue(val value: NamedRecursiveBlock) : HierarchicAttributeValue() {
    override operator fun contains(child: HierarchicAttributeValue) = value.contains((child as SimpleHierarchicValue).value.name)
}