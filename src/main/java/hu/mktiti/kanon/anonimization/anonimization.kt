package hu.mktiti.kanon.anonimization

import hu.mktiti.kanon.attribute.Attribute
import hu.mktiti.kanon.attribute.AttributeType
import hu.mktiti.kanon.attribute.AttributeValue
import hu.mktiti.kanon.attribute.RecordDescriptor
import hu.mktiti.kanon.logger
import java.util.logging.Level

/**
 * Data anonimization algoritm (strategy pattern)
 */
interface FullDataAnonimizationAlgorithm {

    fun anonimize()

}

typealias Record = List<Any>
typealias Data = List<Record>

/**
 * Anonimization core singleton engine
 * Applies algorithms and gives information of the data.
 */
object AnonimizationEngine {

    private val log by logger()

    /**
     * Calculate k-anonimity of the data
     *
     * @param descriptor the data descriptor
     * @param data the data
     * @return k-anonimity of the data
     */
    fun calculateKAnonimity(descriptor: RecordDescriptor, data: List<Record>): Int =
        splitToEqClasses(descriptor, data).map(List<Record>::size).min() ?: 0


    /**
     * Split to equality classes
     *
     * @param descriptor the data descriptor
     * @param data the data
     * @return blocks representing equality classes
     */
    fun splitToEqClasses(descriptor: RecordDescriptor, data: List<Record>): List<Data> {
        val quasiIndexes = descriptor.attributes
                                .mapIndexed(::Pair)
                                .filter { (_, a) -> a.quasiIdentifier }
                                .map(Pair<Int, Attribute<*>>::first)

        val newBlocks: MutableList<Data> = mutableListOf()
        var currentBlocks = listOf(data)
        for (qi in quasiIndexes) {
            newBlocks.clear()
            for (block in currentBlocks) {
                newBlocks.addAll(splitByColumn(block, qi, descriptor.attributes[qi].type).values)
            }
            currentBlocks = ArrayList<Data>(newBlocks)
        }

        log.info {
            currentBlocks.joinToString(
                    prefix = "K-Anonim groups:\n",
                    separator = "\n-----------------\n") {
                it.joinToString(separator = "\n", transform = descriptor::showLine)
            }
        }

        return currentBlocks
    }

    /**
     * Split an equality class further by a previously unused quasi identifier attribute
     *
     * @param T the type of the column on which to base the splitting
     * @param data a record block which represents an equality class in regards to previous quasi identifiers
     * @param attribIndex index of the used column
     * @param attribType type of the used column
     * @return a mapping of the different values of the used column to their respective equality classes
     */
    private fun <T : AttributeValue> splitByColumn(data: Data, attribIndex: Int, attribType: AttributeType<T>): Map<T, Data> {
        val result = HashMap<T, MutableList<Record>>()

        for (record in data) {
            val quasiValue = record[attribIndex] as T
            val key = result.keys.find { attribType.subsetOf(it, quasiValue) }
            if (key == null) {
                result[quasiValue] = mutableListOf(record)
            } else {
                result[key]!!.add(record)
            }
        }

        return result
    }

}