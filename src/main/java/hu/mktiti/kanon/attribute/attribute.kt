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
abstract class AttributeType<T : AttributeValue> {
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

    abstract fun smallestGeneralization(values: List<T>): T

    internal fun partition(values: List<T>): Partition<T> = Partition(values, smallestGeneralization(values))

    abstract fun split(partition: Partition<T>, kValue: Int): PartitionSplit<T>?
}

class AttributeParseException(message: String) : RuntimeException(message)

enum class AttributeQualifier { NONE, QUASI, SECRET, SECRET_KEY }

data class Attribute<T : AttributeValue>(val name: String, val type: AttributeType<T>, val qualifier: AttributeQualifier) {
    override fun toString() = "{$name: $type}"
}

data class Partition<out T : AttributeValue>(val values: List<T>, val aggregateValue: T)

/**
 * Describes the structure of a data input
 *
 * @param attributes list of attributes
 */
data class RecordDescriptor(val attributes: List<Attribute<*>>) {
    private val log by logger()

    constructor(vararg attributes: Attribute<*>) : this(attributes.toList())

    /**
     * Parses line to attribute values
     */
    fun parseLine(line: String): List<Any> {
        val split = line.split(',')
        if (split.size != attributes.size) {
            throw AttributeParseException("Line '$line' has ${split.size} attributes, but ${attributes.size} is required!")
        }

        return try {
            (0 until attributes.size).map {
                attributes[it].type.parse(split[it])
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
        if (tuple.size != attributes.size) throw AttributeParseException("Tuple size doesn't equal number of attributes")

        return try {
            tuple.zip(attributes).joinToString { (t, a) ->
                a.type.showUnsafe(t)
            }
        } catch (tce: TypeCastException) {
            log.log(Level.WARNING, "Illegal attribute type", tce)
            throw AttributeParseException("Unable to print line")
        }
    }
}