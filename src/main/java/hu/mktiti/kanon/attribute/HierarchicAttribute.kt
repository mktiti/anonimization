package hu.mktiti.kanon.attribute

import hu.mktiti.kanon.NamedRecursiveBlock
import hu.mktiti.kanon.RecursiveBlock
import java.util.*

data class MutablePair<K, V>(val key: K, var value: V)

/**
 * Attribute to represent (structured) enums
 *
 * @property valueSet root of accepted Enum values
 */
class HierarchicAttribute(
        private val valueSet: NamedRecursiveBlock) : QuasiAttributeType<HierarchicAttributeValue>() {

    private val reversed: List<List<Pair<NamedRecursiveBlock, String>>>

    init {
        val reversed = LinkedList<List<Pair<NamedRecursiveBlock, String>>>()
        fun addLevel(children: List<Pair<String, List<NamedRecursiveBlock>>>) {
            if (children.isNotEmpty()) {
                reversed.addFirst(children.flatMap { (parent, values) -> values.map { it to parent } })
                val newChildren = children.flatMap { it.second }.map { it.name to it.content }
                addLevel(newChildren)
            }
        }
        addLevel(listOf("" to listOf(valueSet)))
        this.reversed = reversed
    }

    override fun parse(string: String): HierarchicAttributeValue {
        return valueSet.find(string)?.let(::SimpleHierarchicValue) ?:
        throw AttributeParseException("Enum value [$string] not in set of possible values [$valueSet]")
    }

    override fun toString() = "${valueSet.name} (enum) attribute"

    override fun show(value: HierarchicAttributeValue): String =
            searchPath((value as SimpleHierarchicValue).value.name).joinToString(separator = ".", transform = NamedRecursiveBlock::name)

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
        return when (values.size) {
            0 -> SimpleHierarchicValue(valueSet)
            1 -> values[0]
            else -> values.reduce { a, b ->
                val aFull = searchPath(a.value.name, valueSet)
                val bFull = searchPath(b.value.name, valueSet)

                for (i in 0 until minOf(aFull.size, bFull.size)) {
                    if (aFull[i] != bFull[i]) {
                        return@reduce SimpleHierarchicValue(aFull[i - 1])
                    }
                }

                return@reduce a
            }
        }

    }

    override fun partitionError(partition: Partition<HierarchicAttributeValue>): Double {
        val nodeCount = partition.values.groupBy { it.value.name }.mapValues { it.value.size }

        fun sumRatioCount(node: NamedRecursiveBlock, rootValue: Double = 1.toDouble()): Double {
            val nodeVal = (nodeCount[node.name] ?: 0) / rootValue
            val childrenRoot = rootValue * node.content.size
            return nodeVal + node.content.map { sumRatioCount(it, childrenRoot) }.sum()
        }

        return sumRatioCount(partition.aggregateValue.value)
    }

    override fun splitToParts(partition: Partition<HierarchicAttributeValue>, kValue: Int): Pair<List<HierarchicAttributeValue>, List<HierarchicAttributeValue>>? {
        if (partition.values.size < kValue * 2) return null
        assert(kValue >= 1)

        val nodeCount = partition.values.groupBy { it.value.name }.mapValues { it.value.size }
        val withCount = valueSet.map { it to (nodeCount[it] ?: 0) }

        fun sumMapChildren(node: RecursiveBlock<Pair<String, Int>>): RecursiveBlock<Pair<String, Int>> {
            val summedChildren = node.content.map { sumMapChildren(it) }
            return RecursiveBlock(node.key.first to (node.key.second + summedChildren.sumBy { it.key.second }), summedChildren)
        }

        val summedValues = sumMapChildren(withCount)

        fun findValidCut(node: RecursiveBlock<Pair<String, Int>>): RecursiveBlock<Pair<String, Int>>? {
            if (node.key.second < kValue) return null

            return node.content.filter { it.key.second >= kValue }
                        .mapNotNull { findValidCut(it) }
                        .minBy { it.key.second } ?: node
        }

        val selectedCut = findValidCut(summedValues) ?: return null

        val (selected, remaining) =
                partition.values.partition {
                    selectedCut.contains(it.value.name) { it.key.first }
                }

        if (selected.size < kValue || remaining.size < kValue) return null

        return selected to remaining
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