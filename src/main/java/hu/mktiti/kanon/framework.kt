package hu.mktiti.kanon

import hu.mktiti.kanon.anonimization.AnonimizationEngine
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.InputStream

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
                AnonimizationEngine.anonimizeStream(config)
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

    // Mock start from IDE
    FileInputStream("data.csv").use {
        System.setIn(it)
        Framework.main(args)
    }

}
