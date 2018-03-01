package hu.mktiti.kanon

import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class NamedRecursiveBlock(name: String, content: List<NamedRecursiveBlock>) : NamedBlock<NamedRecursiveBlock>(name, content) {
    override fun toString() =
            if (content.isEmpty())
                name
            else
                name + content.joinToString(prefix = " {", separator = ", ", postfix = "}")

    fun contains(value: String): Boolean = name == value || content.any { it.contains(value) }
}

internal object DescriptorParser {
    private val log by logger()

    internal fun parse(filePath: String): RecordDescriptor? {
        val file = File(filePath)
        if (!file.exists()) {
            log.warning("descriptor file '$filePath' does not exists!")
            return null
        }

        val list = try {
            parseContentList(file.readText())
        } catch (iae: IllegalArgumentException) {
            log.warning("failed to parse descriptor file '${file.absolutePath}'")
            log.warning(iae.message)
            return null
        }

        val enums = list.firstOrNull { it.name == "Enums" }?.content ?: emptyList()
        log.info("Hierarchical enum types:")
        enums.map(NamedRecursiveBlock::toString).forEach(log::info)

        val attributesBlock = list.firstOrNull { it.name == "Attributes" }
        if (attributesBlock == null) {
            log.warning("Attributes block cannot be found")
            return null
        }

        val attributes = parseAttributes(attributesBlock.content.joinToString(separator = "\n") { it.name }, enums)
        return attributes?.let(::RecordDescriptor)
    }

    private fun parseAttributes(content: String, enums: List<NamedRecursiveBlock>): List<Attribute<*>>? {
        try {
            return content.lines()
                        .map(String::trim)
                        .filter { it.isNotEmpty() && !it.startsWith("#") }
                        .map { parseLine(it, enums) }
        } catch (ce: ConfigException) {
            log.warning("Unable to correctly parse attributes")
            log.warning(ce.message)
        }

        return null
    }

    private fun parseLine(line: String, enums: List<NamedRecursiveBlock>): Attribute<*> {
        try {
            val tokens: MutableList<String> = LinkedList(line.trim().split("\\s+".toRegex()))
            val name = tokens.removeAt(0)
            val quasi = tokens.remove("quasi")
            val secret = tokens.remove("secret")
            val type = tokens.removeAt(0)

            val typeParams = tokens.joinToString(prefix = "", separator = "", postfix = "")

            val parsed = when (type) {
                "Int"       -> parseInt(typeParams)
                "String"    -> parseString(typeParams)
                "Date"      -> parseDate(typeParams)
                else        -> {
                    enums.find { it.name == type }?.let(::HierarchicAttribute) ?: throw ConfigException("unsupported type '$type'")
                }
            }
            return Attribute(name, parsed, quasi, secret)

        } catch (ioe: IndexOutOfBoundsException) {
            throw ConfigException("Invalid line '$line', parameter missing")
        }
    }

    private fun parseInt(params: String): AttributeType<Int> =
            fromBrackets(params, parse = { it.toInt() },
                    missing = { IntAttribute() },
                    left = { IntAttribute(minValue = it) },
                    right = { IntAttribute(maxValue = it) },
                    both = { min, max -> IntAttribute(minValue = min, maxValue = max) })

    private fun parseString(params: String): AttributeType<String> =
            fromBrackets(params, parse = { it.toInt() },
                    missing = { StringAttribute() },
                    left = { StringAttribute(minLength = it) },
                    right = { StringAttribute(maxLength = it) },
                    both = { min, max -> StringAttribute(min, max) })

    private fun parseDate(params: String): AttributeType<LocalDate> {
        val tokens = params.split("[")
        val pattern = if (tokens.size != 2 || tokens[0].isEmpty()) "yyyy-MM-dd" else tokens[0]
        val formatter = DateTimeFormatter.ofPattern(pattern)

        return fromBrackets(params, parse = { LocalDate.parse(it, formatter) },
                missing = { DateAttribute() },
                left = { DateAttribute(formatterString = pattern, after = it) },
                right = { DateAttribute(formatterString = pattern, before = it) },
                both = { min, max -> DateAttribute(pattern, min, max) })
    }

    private fun <A, T> fromBrackets(params: String, parse: (String) -> T?,
                                    missing: () -> AttributeType<A>,
                                    left: (T) -> AttributeType<A>,
                                    both: (T, T) -> AttributeType<A>,
                                    right: (T) -> AttributeType<A>): AttributeType<A> {

        val (leftVal, rightVal) = parseBrackets(params, parse) ?: return missing()
        return if (leftVal != null && rightVal != null) {
            both(leftVal, rightVal)
        } else if (leftVal != null) {
            left(leftVal)
        } else if (rightVal != null) {
            right(rightVal)
        } else {
            missing()
        }
    }

    private fun <T> parseBrackets(params: String, parse: (String) -> T?): Pair<T?, T?>? {
        var cleaned = params.trim()
        if (cleaned.startsWith("[")) cleaned = cleaned.drop(1)
        if (cleaned.endsWith("]"))   cleaned = cleaned.dropLast(1)

        cleaned = cleaned.trim()
        if (cleaned.isEmpty()) return null
        if (cleaned.startsWith(";")) return null to parse(cleaned.drop(1).trim())
        if (cleaned.endsWith(";")) return parse(cleaned.dropLast(1).trim()) to null

        val tokens = cleaned.split(";").map(String::trim).filter(String::isNotEmpty)
        if (tokens.size == 2) return parse(tokens[0]) to parse(tokens[1])

        throw ConfigException("cannot parse type parameter $params")
    }

}