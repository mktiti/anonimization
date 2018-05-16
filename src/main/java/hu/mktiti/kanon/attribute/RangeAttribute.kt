package hu.mktiti.kanon.attribute

import java.util.*

interface RangeAttributeValue<out T> : AttributeValue {
    fun min(): T

    fun max(): T

    fun rangeSize(): Long
}

abstract class RangeAttribute<T : Comparable<T>, AV : RangeAttributeValue<T>>(
        protected val minValue: T,
        protected val maxValue: T) : QuasiAttributeType<AV>() {

    protected fun simplify(min: T, max: T): AV = if (min == max) simpleValue(min) else rangeValue(min, max)

    abstract fun simpleValue(value: T): AV

    abstract fun rangeValue(min: T, max: T): AV

    protected fun safeInRange(value: T): T
            = if (value in minValue..maxValue) value else throw AttributeParseException("Value [$value] out of range [$minValue;$maxValue]")

    abstract override fun parse(string: String): AV

    override fun toString() =
            "Range attribute [${if (minValue == Int.MIN_VALUE) "" else minValue.toString()};${if (maxValue == Int.MAX_VALUE) "" else maxValue.toString()}]"

    abstract override fun show(value: AV): String

    override fun subsetOf(parent: AV, child: AV) = child.min() >= parent.min() && child.max() <= parent.max()

    private fun minimum(range: List<AV>) = range.map { it.min() }.min() ?: minValue

    private fun maximum(range: List<AV>) = range.map { it.max() }.max() ?: maxValue

    override fun smallestGeneralization(values: List<AV>) = simplify(minimum(values), maximum(values))

    override fun splitToParts(partition: Partition<AV>, kValue: Int): Pair<List<AV>, List<AV>>? {
        if (partition.values.size < 2 * kValue) return null

        val bigger = LinkedList(partition.values)
        bigger.sortWith(compareBy({ it.min() }, { it.max() }))
        val smaller = ArrayList<AV>(kValue)

        while (smaller.size < kValue && bigger.size >= kValue) {
            var currentMax = bigger.first.max()
            while (bigger.isNotEmpty() && bigger.first.min() <= currentMax) {
                val new = bigger.pop()
                smaller.add(new)
                if (new.max() > currentMax) {
                    currentMax = new.max()
                }
            }
        }

        if (smaller.size >= kValue && bigger.size >= kValue) {
            return smaller to bigger
        }

        return null
    }

    override fun partitionError(partition: Partition<AV>): Double {
        val partitionSize = partition.aggregateValue.rangeSize()
        return partition.values.map { partitionSize / it.rangeSize().toDouble() }.average() // Mean error
    }
}