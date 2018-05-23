package hu.mktiti.kanon.anonimization

import hu.mktiti.kanon.StreamConfig
import hu.mktiti.kanon.logger
import java.io.OutputStream
import java.io.PrintStream

class StreamAnonimizator(private val config: StreamConfig, outputStream: OutputStream) : StreamAnonimizationAlgorithm {
    private val log by logger()

    private val outputStream = PrintStream(outputStream)

    private val storedPartitions: MutableList<RecordPartition> = mutableListOf()

    private val storedCount: Int
        get() = storedPartitions.sumBy(RecordPartition::size)

    private var initialBatch = true

    private fun releaseAllStored() {
        storedPartitions.map { it.releaseAll(outputStream) }
    }

    override fun processRow(row: Record) {
        log.info { "Processing row: ${config.descriptor.showLine(row)}" }

        if (initialBatch) {
            processRowInitialBatch(row)
        } else {
            processRowSecondary(row)
        }

        if (storedCount == config.storedLimit) {
            if (initialBatch) {
                processInitialBatch()
                //initialBatch = false
                storedPartitions.clear()
            } else {
                processBatch()
            }
        }
    }

    private fun processRowInitialBatch(row: Record) {
        val universalPartition = storedPartitions.firstOrNull()

        if (universalPartition == null) {
            storedPartitions.add(RecordPartition(config.descriptor, config.kValue, config.storedLimit, listOf(row)))
        } else {
            universalPartition.add(row)
        }
    }

    private fun processRowSecondary(row: Record) {
        val containingPartition = storedPartitions.asSequence().firstOrNull { it.contains(row) }

        if (containingPartition == null) {

        } else {
            containingPartition.add(row)
        }
    }

    private fun processInitialBatch() {
        val universalPartition = storedPartitions.first()
        storedPartitions.clear()
        storedPartitions.addAll(universalPartition.splitRecursively())

        val parts = universalPartition.splitRecursively()
        parts.map { it.releaseAll(outputStream) }
    }

    private fun processBatch() {

    }

    override fun close() {
        if (storedPartitions.isNotEmpty()) {
            processInitialBatch()
        }

        log.info("Anonimization finished")
    }
}