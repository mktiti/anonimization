package hu.mktiti.kanon.attribute

import hu.mktiti.kanon.logger
import java.util.logging.Level

interface AttributeValue

data class PartitionSplit<out T : AttributeValue>(val partitionA: Partition<T>, val partitionB: Partition<T>, val informationGainValue: Double)

/**
 * Represents a column of a record
 *
 * @param T the type of data to store
 */
abstract class QuasiAttributeType<T : AttributeValue> {
    /**
     * Parse attribute data from String
     */
    abstract fun parse(string: String): T

    /**
     * Convert attribute data to String
     */
    open fun show(value: T): String = value.toString()

    /**
     * Cast and convert attribute data to String
     */
    fun showUnsafe(value: Any): String = show(value as T)

    /**
     * Check if attribute value is within another attribute value (used for data anonimization by stubbing)
     */
    abstract fun subsetOf(parent: T, child: T): Boolean

    fun unsafeSubsetOf(parent: Any, child: Any) = subsetOf(parent as T, child as T)

    abstract fun smallestGeneralization(values: List<T>): T

    internal open fun partition(values: List<T>): Partition<T> = Partition(values, smallestGeneralization(values))

    fun unsafePartition(values: Any) = partition(values as List<T>)

    internal open fun singletonPartition(value: T) = Partition(listOf(value), value)

    internal open fun unsafeSingletonPartition(value: Any) = singletonPartition(value as T)

    protected abstract fun splitToParts(partition: Partition<T>, kValue: Int): Pair<List<T>, List<T>>?

    protected abstract fun partitionError(partition: Partition<T>): Double

    fun split(partition: Partition<T>, kValue: Int): PartitionSplit<T>? {
        val (smaller, bigger) = splitToParts(partition, kValue) ?: return null
        val partA = partition(smaller)
        val partB = partition(bigger)

        val errorA = partitionError(partA)
        val errorB = partitionError(partB)
        val errorCombined = partitionError(partition)

        return PartitionSplit(partA, partB, 2 * errorCombined - errorA - errorB)
    }
}

class AttributeParseException(message: String) : RuntimeException(message)

sealed class Attribute(val position: Int, val name: String)

class QuasiAttribute<T : AttributeValue>(position: Int, name: String, val type: QuasiAttributeType<T>) : Attribute(position, name) {
    override fun toString() = "{$name: $type}"
}

class SecretAttribute(position: Int, name: String) : Attribute(position, name) {
    override fun toString() = "{$name}"
}

data class Partition<out T : AttributeValue>(val values: List<T>, val aggregateValue: T)

/**
 * Describes the structure of a data input
 *
 * @param attributes list of attributes
 */
data class RecordDescriptor(val quasiAttributes: List<QuasiAttribute<*>>, val secretAttributes: List<SecretAttribute>) {
    private val log by logger()

    val allAttributes: List<Attribute> = with(mutableListOf<Attribute>()) {
        addAll(quasiAttributes)
        addAll(secretAttributes)
        sortedBy(Attribute::position)
    }

    /**
     * Parses line to attribute values
     */
    fun parseLine(line: String): List<Any> {
        val split = line.split(';')
        if (split.size != allAttributes.size) {
            throw AttributeParseException("Line '$line' has ${split.size} attributes, but ${allAttributes.size} is required!")
        }

        return try {
            (0 until allAttributes.size).map {
                val attrib = allAttributes[it]
                when (attrib) {
                    is QuasiAttribute<*> -> attrib.type.parse(split[it])
                    is SecretAttribute -> split[it]
                }
            }.toList()
        } catch (ape: AttributeParseException) {
            log.log(Level.WARNING, "Unable to parse attribute", ape)
            throw AttributeParseException("Unable to parse line")
        }
    }

    /**
     * Convert attribute values to String representation
     */
    fun showLine(tuple: List<Any>): String {
        if (tuple.size != allAttributes.size) throw AttributeParseException("Tuple size doesn't equal number of attributes")

        return try {
            tuple.zip(allAttributes).joinToString { (t, a) ->
                when (a) {
                    is QuasiAttribute<*> -> a.type.showUnsafe(t)
                    is SecretAttribute -> t.toString()
                }
            }
        } catch (tce: TypeCastException) {
            log.log(Level.WARNING, "Illegal attribute type", tce)
            throw AttributeParseException("Unable to print line")
        }
    }
}