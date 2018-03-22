package hu.mktiti.kanon

import hu.mktiti.kanon.anonimization.AnonimizationEngine

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

fun main(args: Array<String>) {
    Framework.main(args)
}
