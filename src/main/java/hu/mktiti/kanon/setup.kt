package hu.mktiti.kanon

import hu.mktiti.kanon.attribute.RecordDescriptor
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import java.io.File

/**
 * Program configuration
 *
 * @property descriptor record descriptor
 */
sealed class Config(val descriptor: RecordDescriptor)

class FileBasedConfig(
        descriptor: RecordDescriptor,
        val dataFile: File,
        val outputFilePath: String) : Config(descriptor)

class StreamConfig(
        descriptor: RecordDescriptor,
        val batchSize: Int) : Config(descriptor)

private enum class ArgOption(
        val fullName: String,
        val shortName: String,
        val description: String,
        val withParameter: Boolean = true,
        val defaultParameter: String? = null) {

    DESCRIPTOR("descriptor", "d", "the data descriptor file", defaultParameter = "descriptor.conf"),
    DATAFILE("datafile", "f", "the data file to be anonymized", defaultParameter = "data.csv"),
    OUTPUT("output", "o", "anonymization result output filepath", defaultParameter = "output.csv"),
    USE_STDIO("stdio", "s", "use the standard I/O system for streamed data processing", defaultParameter = "", withParameter = false),
    BATCH_SIZE("batch-size", "b", "batch size for stream processing", defaultParameter = "100", withParameter = false)

}

internal class ConfigException(message: String) : RuntimeException(message)

/**
 * Parser engine singleton
 */
internal object Parser {
    private val log by logger()

    private fun createCliSetup(): Options = Options().apply {
        ArgOption.values().forEach {
            addOption(it.shortName, it.fullName, it.withParameter, it.description)
        }
    }

    /**
     * Safely parse configuration
     *
     * @param args program arguments
     * @return the config if it can be parsed and is valid
     */
    internal fun parseConfig(args: Array<String>): Config? {
        try {
            val commandLine = DefaultParser().parse(createCliSetup(), args)

            val descriptor = DescriptorParser.parse(getParameter(commandLine, ArgOption.DESCRIPTOR)) ?: return null
            val useStdIO = commandLine.hasOption(ArgOption.USE_STDIO.fullName)

            if (useStdIO) {
                val batchSize = getParameter(commandLine, ArgOption.BATCH_SIZE).toIntOrNull() ?: throw ParseException("Batch size must be an integer");
                if (batchSize <= 1) throw ParseException("Batch size must be > 1")

                return StreamConfig(descriptor, batchSize)
            } else {
                val dataFile = File(getParameter(commandLine, ArgOption.DATAFILE))

                if (!dataFile.exists()) {
                    log.warning("datafile '${dataFile.absoluteFile}' doesn't exist")
                    return null
                }

                return FileBasedConfig(descriptor, dataFile, getParameter(commandLine, ArgOption.OUTPUT))
            }
        } catch (pe: ParseException) {
            log.warning("Invalid command line arguments!")
            log.warning(pe.message)
            return null
        }
    }

    private fun getParameter(commandLine: CommandLine, option: ArgOption): String =
            coalesce({ commandLine.getOptionValue(option.fullName) },
                     { option.defaultParameter },
                     default = { throw ParseException("No parameter for ${option.fullName}") })

}