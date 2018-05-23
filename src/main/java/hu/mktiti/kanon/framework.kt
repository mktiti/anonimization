package hu.mktiti.kanon

import hu.mktiti.kanon.anonimization.AnonimizationEngine
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.PrintStream

/**
 * Framework engine singleton
 */
object Framework {

    private val log by logger()

    fun main(args: Array<String>) {

        val config = Parser.parseConfig(args)
        if (config == null) {
            log.warning("Invalid configuration, exiting")
            System.exit(1)
            return
        }

        when (config) {
            is StreamConfig -> {
                AnonimizationEngine.anonymizeStream(config)
            }

            is FileBasedConfig -> {
                config.dataFile.useLines { lines ->
                    val data = lines
                            .filterNot(String::isBlank)
                            .filterNot { it.trimStart().startsWith("#") }
                            .map(config.descriptor::parseLine).toList()

                    println("K-Anonimity: ${AnonimizationEngine.calculateKAnonimity(config.descriptor, data)}")

                    println(data.joinToString(separator = "\n", transform = config.descriptor::showLine))
                }
            }
        }

    }
}

fun main(args: Array<String>) {
    //Framework.main(args)

    // Start with mock input data stream from IDE
    // Same as piping 'data.csv' into the program and redirecting std err to 'log.txt'
    // cat data.csv | java hu.mktiti.kanon.Framework 2> log.txt
    FileInputStream("data.csv").use { inStream ->
        PrintStream(FileOutputStream("log.txt")).use { logStream ->
            System.setIn(inStream)
            System.setErr(logStream)
            Framework.main(args)
        }
    }

}
