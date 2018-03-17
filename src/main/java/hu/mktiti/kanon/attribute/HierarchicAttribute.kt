package hu.mktiti.kanon.attribute

import hu.mktiti.kanon.NamedRecursiveBlock
import java.util.*

class HierarchicAttribute(
        private val valueSet: NamedRecursiveBlock) : AttributeType<String>() {

    override fun parser(): (String) -> String = {
        if (valueSet.contains(it)) it
        else throw AttributeParseException("Enum value [$it] not in set of possible values [$valueSet]")
    }

    override fun toString() = "${valueSet.name} (enum) attribute"

    override fun show(value: String): String =
            searchPath(value)?.joinToString(separator = ".", transform = NamedRecursiveBlock::name) ?: "---"

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

    override fun isSame(a: String, b: String) = a == b
}