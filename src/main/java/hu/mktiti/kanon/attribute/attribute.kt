package hu.mktiti.kanon.attribute

import hu.mktiti.kanon.logger
import java.util.logging.Level

interface AttributeValue

abstract class AttributeType<T : AttributeValue> {
    abstract fun parse(string: String): T

    open fun show(value: T): String = value.toString()

    fun showUnsafe(value: Any): String = show(value as T)

    abstract fun subsetOf(parent: T, child: T): Boolean
}

class AttributeParseException(message: String) : RuntimeException(message)

data class Attribute<T : AttributeValue>(val name: String, val type: AttributeType<T>, val quasiIdentifier: Boolean, val secret: Boolean) {

    override fun toString() = "{$name: $type}"
}

data class RecordDescriptor(val attributes: List<Attribute<*>>) {
    private val log by logger()

    constructor(vararg attributes: Attribute<*>) : this(attributes.toList())

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