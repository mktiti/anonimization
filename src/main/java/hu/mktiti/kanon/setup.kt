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
sealed class Config(val descriptor: RecordDescriptor,
                    val kValue: Int)

class FileBasedConfig(
        descriptor: RecordDescriptor,
        kValue: Int,
        val dataFile: File,
        val outputFilePath: String) : Config(descriptor, kValue)

class StreamConfig(
        descriptor: RecordDescriptor,
        kValue: Int,
        val storedLimit: Int) : Config(descriptor, kValue)

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
    K_VALUE("k-value", "k", "K anonymizational value to reach", defaultParameter = "50"),
    STORED_RECORD_LIMIT("stored-limit", "l", "Upper limit of records to store for stream anonymization", defaultParameter = "1000")

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
            val kValue = getIntParameter(commandLine, ArgOption.K_VALUE)
            val useStdIO = commandLine.hasOption(ArgOption.USE_STDIO.fullName)

            if (useStdIO) {
                return StreamConfig(descriptor, kValue, getIntParameter(commandLine, ArgOption.STORED_RECORD_LIMIT))
            } else {
                val dataFile = File(getParameter(commandLine, ArgOption.DATAFILE))

                if (!dataFile.exists()) {
                    log.warning("datafile '${dataFile.absoluteFile}' doesn't exist")
                    return null
                }

                return FileBasedConfig(descriptor, kValue, dataFile, getParameter(commandLine, ArgOption.OUTPUT))
            }
        } catch (pe: ParseException) {
            log.warning("Invalid command line argument!")
            log.warning(pe.message)
            return null
        }
    }

    private fun getIntParameter(commandLine: CommandLine, option: ArgOption, range: Pair<Int?, Int?> = Pair(1, null)): Int {
        val value = getParameter(commandLine, option).toIntOrNull() ?: throw ParseException("Invalid number for parameter '${option.fullName}'")
        val (min, max) = range
        if (min != null && value < min) throw ParseException("Value for '${option.fullName}' must be >= $min")
        if (max != null && value > max) throw ParseException("Value for '${option.fullName}' must be <= $max")
        return value
    }

    private fun getParameter(commandLine: CommandLine, option: ArgOption): String =
            coalesce({ commandLine.getOptionValue(option.fullName) },
                     { option.defaultParameter },
                     default = { throw ParseException("No parameter for ${option.fullName}") })

}