package hu.mktiti.kanon

import hu.mktiti.kanon.attribute.RecordDescriptor
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import java.io.File

internal data class Config(
        val descriptor: RecordDescriptor,
        val dataFile: File,
        val outputFilePath: String)

private enum class ArgOption(
        val fullName: String,
        val shortName: String,
        val description: String,
        val withParameter: Boolean = true,
        val defaultParameter: String? = null) {

    DESCRIPTOR("descriptor", "d", "the data descriptor file", defaultParameter = "descriptor.conf"),
    DATAFILE("datafile", "f", "the data file to be anonymized", defaultParameter = "data.csv"),
    OUTPUT("output", "o", "anonymization result output filepath", defaultParameter = "output.csv")

}

internal class ConfigException(message: String) : RuntimeException(message)

internal object Parser {
    private val log by logger()

    private fun createCliSetup(): Options = Options().apply {
        ArgOption.values().forEach {
            addOption(it.shortName, it.fullName, it.withParameter, it.description)
        }
    }

    internal fun parseConfig(args: Array<String>): Config? {
        try {
            val commandLine = DefaultParser().parse(createCliSetup(), args)

            val descriptor = DescriptorParser.parse(getParameter(commandLine, ArgOption.DESCRIPTOR)) ?: return null
            val dataFile = File(getParameter(commandLine, ArgOption.DATAFILE))

            if (!dataFile.exists()) {
                log.warning("datafile '${dataFile.absoluteFile}' doesn't exist")
                return null
            }

            return Config(descriptor, dataFile, getParameter(commandLine, ArgOption.OUTPUT))
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