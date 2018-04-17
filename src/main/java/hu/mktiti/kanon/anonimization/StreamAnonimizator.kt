package hu.mktiti.kanon.anonimization

import hu.mktiti.kanon.StreamConfig
import hu.mktiti.kanon.logger
import java.io.OutputStream

object StreamAnonimizator : StreamAnonimizationAlgorithm {
    private val log by logger()

    lateinit var config: StreamConfig
    lateinit var outputStream: OutputStream

    override fun init(config: StreamConfig, outputStream: OutputStream) {
        this.config = config
        this.outputStream = outputStream
    }

    override fun processRow(row: Record) {
        log.info { "Processing row: ${config.descriptor.showLine(row)}" }
    }

    override fun close() {
        log.info("Anonimization finished")
    }
}