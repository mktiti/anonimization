package hu.mktiti.kanon.anonimization

import hu.mktiti.kanon.attribute.Attribute
import hu.mktiti.kanon.attribute.AttributeType
import hu.mktiti.kanon.attribute.RecordDescriptor
import hu.mktiti.kanon.logger
import java.util.logging.Level

interface FullDataAnonimizationAlgorithm {

    fun anonimize()

}

typealias Record = List<Any>
typealias Data = List<Record>

object AnonimizationEngine {

    private val log by logger()

    fun calculateKAnonimity(descriptor: RecordDescriptor, data: List<Record>): Int {
        val quasiIndexes = descriptor.attributes
                            .mapIndexed { i, a -> i to a }
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

        if (log.isLoggable(Level.INFO)) {
            StringBuilder().apply {
                appendln("K-Anonim groups:")
                for (group in currentBlocks) {
                    appendln("--------------------")
                    for (record in group) {
                        appendln(descriptor.showLine(record))
                    }
                }
                log.info(toString())
            }
        }

        return currentBlocks.map(List<Record>::size).min() ?: 0
    }

    private fun <T : Any> splitByColumn(data: Data, attribIndex: Int, attribType: AttributeType<T>): Map<T, Data> {
        val result = HashMap<T, MutableList<Record>>()

        for (record in data) {
            val quasiValue = record[attribIndex] as T
            val key = result.keys.find { attribType.isSame(it, quasiValue) }
            if (key == null) {
                result[quasiValue] = mutableListOf(record)
            } else {
                result[key]!!.add(record)
            }
        }

        return result
    }

}