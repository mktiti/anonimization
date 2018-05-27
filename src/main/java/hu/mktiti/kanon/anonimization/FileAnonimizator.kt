package hu.mktiti.kanon.anonimization

import hu.mktiti.kanon.FileBasedConfig
import hu.mktiti.kanon.logger
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.FileReader
import java.io.PrintStream

class FileAnonimizator(private val config: FileBasedConfig) : FullDataAnonimizationAlgorithm {
    private val log by logger()

    override fun anonymize() {
        val partition = RecordPartition(config.descriptor, config.kValue, 10, listOf())

        log.info("Reading data file")
        BufferedReader(FileReader(config.dataFile)).useLines { lines ->
            lines.forEach { line ->
                if (line.isNotBlank() && !line.trimStart().startsWith("#")) {
                    partition.add(config.descriptor.parseLine(line))
                }
            }
        }

        log.info("Running anonymization algorithm")
        val equalityClasses = partition.splitRecursively()

        log.info("Writing equality classes to ${config.outputFilePath}")
        PrintStream(FileOutputStream(config.outputFilePath)).use { out ->
            equalityClasses.forEach { eqClass ->
                eqClass.releaseAll(out)
            }
        }
    }

}