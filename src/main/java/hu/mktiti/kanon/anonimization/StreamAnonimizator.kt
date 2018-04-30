package hu.mktiti.kanon.anonimization

import hu.mktiti.kanon.StreamConfig
import hu.mktiti.kanon.attribute.Attribute
import hu.mktiti.kanon.attribute.AttributeQualifier
import hu.mktiti.kanon.attribute.AttributeValue
import hu.mktiti.kanon.attribute.Partition
import hu.mktiti.kanon.logger
import java.io.OutputStream
import java.io.PrintStream
import java.util.*

class StreamAnonimizator(private val config: StreamConfig, outputStream: OutputStream) : StreamAnonimizationAlgorithm {
    private val log by logger()

    private val outputStream = PrintStream(outputStream)

    private val releasedRecords = LinkedList<Record>()
    private val currentBatch = LinkedList<Record>()

    override fun processRow(row: Record) {
        log.info { "Processing row: ${config.descriptor.showLine(row)}" }

        currentBatch.addFirst(row)
        if (currentBatch.size == config.batchSize) {
            processBatch()
        }
    }

    private fun processBatch() {
        currentBatch.reverse()

        val bestSplit = config.descriptor.attributes.filter {
            it.qualifier == AttributeQualifier.QUASI
        }.mapIndexed { i, attribute ->
            val partition = attribute.type.partition(currentBatch.map { it[i] } as List<Nothing>) as Partition<Nothing>
            val split = attribute.type.split(partition, config.kValue)
            i to (split ?: return@mapIndexed null)
        }.filterNotNull().maxBy {
            it.second.informationGainValue
        }

        if (bestSplit == null) {
            log.warning("Cannot split partition. This is an error in the program")
        } else {
            val (partA, partB) = currentBatch.partition { record ->
                record[bestSplit.first] in bestSplit.second.partitionA.values
            }


        }

        currentBatch.clear()
    }

    override fun close() {
        processBatch()

        log.info("Anonimization finished")
    }
}