package hu.mktiti.kanon.anonimization

import hu.mktiti.kanon.StreamConfig
import hu.mktiti.kanon.logger
import java.io.OutputStream
import java.io.PrintStream

class StreamAnonimizator(private val config: StreamConfig, outputStream: OutputStream) : StreamAnonimizationAlgorithm {
    private val partitionHoldbackRatio = 0.1

    private val log by logger()

    private val outputStream = PrintStream(outputStream)

    private val storedPartitions: MutableList<RecordPartition> = mutableListOf()
    private var newPartition: RecordPartition

    private val storedCount: Int
        get() = storedPartitions.sumBy(RecordPartition::size) + newPartition.size

    init {
        newPartition = RecordPartition(config.descriptor, config.kValue, config.storedLimit, listOf())
    }

    override fun processRow(row: Record) {
        log.info { "Processing row: ${config.descriptor.showLine(row)}" }

        var hasParentPartition = false
        for (stored in storedPartitions) {
            if (stored.contains(row)) {
                stored.add(row)
                hasParentPartition = true
                break
            }
        }

        if (!hasParentPartition) {
            newPartition.add(row)
        }

        if (storedCount >= config.storedLimit) {
            processBatch()
        }
    }

    private fun processBatch() {
        for (stored in storedPartitions) {
            if (stored.size < config.kValue) {
                stored.recordsView().forEach {
                    newPartition.add(it)
                }
            }
        }

        val filteredStored = storedPartitions.filter { it.size >= config.kValue }
        storedPartitions.clear()
        storedPartitions.addAll(filteredStored)

        while (newPartition.size < config.kValue) {
            storedPartitions.removeAt(0).recordsView().forEach {
                newPartition.add(it)
            }
        }


        val equalityClasses = storedPartitions.flatMap { it.splitRecursively() }.toMutableList()
        equalityClasses.addAll(newPartition.splitRecursively())

        equalityClasses.forEach { it.releaseAll(outputStream) }

        newPartition = RecordPartition(config.descriptor, config.kValue, config.storedLimit, listOf())

        val keptPartitions = equalityClasses
                                .sortedByDescending(RecordPartition::errorSum)
                                .take((storedPartitions.size * partitionHoldbackRatio).toInt())
                                .filter { it.size >= config.kValue }
        storedPartitions.clear()
        storedPartitions.addAll(keptPartitions)
        storedPartitions.map {
            it.clear()
        }
    }

    override fun close() {
        if (storedPartitions.isNotEmpty() || !newPartition.isEmpty) {
            this.processBatch()
        }

        log.info("Anonimization finished")
    }
}