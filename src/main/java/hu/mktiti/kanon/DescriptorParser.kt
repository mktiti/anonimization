package hu.mktiti.kanon

import hu.mktiti.kanon.attribute.*
import org.funktionale.either.Either
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

/**
 * Represents recursive string tree node
 * Used for structured enum storage and configuration
 */
class NamedRecursiveBlock(name: String, content: List<NamedRecursiveBlock>) : NamedBlock<NamedRecursiveBlock>(name, content) {
    val name: String
        get() = key

    override fun toString() =
            if (content.isEmpty())
                name
            else
                name + content.joinToString(prefix = " {", separator = ", ", postfix = "}")

    fun contains(value: String): Boolean = name == value || content.any { it.contains(value) }

    /**
     * Searches for value down the tree
     *
     * @param value the name of the node to search for
     * @return the node with the correct name if it exists below this node, otherwise null
     */
    fun find(value: String): NamedRecursiveBlock? {
        return if (name == value) {
            this
        } else {
            content.asSequence().map { it.find(value) }.filterNotNull().firstOrNull()
        }
    }

    fun <T> map(transform: (String) -> T): RecursiveBlock<T> = RecursiveBlock(transform(name), content.map { it.map(transform) })
}

/**
 * Descriptor config file parser singleton
 */
internal object DescriptorParser {
    private val log by logger()

    /**
     * Safely parses the descriptor file
     *
     * @param filePath path to the descriptor
     * @return the config if it can be parsed, otherwise null
     */
    internal fun parse(filePath: String): RecordDescriptor? {
        val file = File(filePath)
        if (!file.exists()) {
            log.warning("descriptor file '${file.absoluteFile}' does not exists!")
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

        val (hierarchicEnums, flatEnumsTree) = enums.partition { it.content.any { child -> child.content.isNotEmpty() } }
        val flatEnums = flatEnumsTree.map { it.name to it.content.map(NamedRecursiveBlock::name) }

        log.info("Hierarchical enum types:")
        hierarchicEnums.map(NamedRecursiveBlock::toString).forEach(log::info)

        log.info("Flat enum types:")
        flatEnums.map { (name, values) -> values.joinToString(prefix = "$name: {", postfix = "}") }
                 .forEach(log::info)

        val attributesBlock = list.firstOrNull { it.name == "Attributes" }
        if (attributesBlock == null) {
            log.warning("Attributes block cannot be found")
            return null
        }

        val attributes = parseAttributes(attributesBlock.content.joinToString(separator = "\n") { it.name }, hierarchicEnums, flatEnums)
        return attributes?.let { (quasis, secrets) -> RecordDescriptor(quasis, secrets) }
    }

    /**
     * Safely parses attribute config
     *
     * @param content the attribute descriptor lines to parse
     * @param hierarchicEnums the list of (structured) hierarchic enums that are defined and can be referenced
     * @param flatEnums the list of flat enums that are defined and can be referenced
     */
    private fun parseAttributes(content: String, hierarchicEnums: List<NamedRecursiveBlock>, flatEnums: List<Pair<String, List<String>>>)
            : Pair<List<QuasiAttribute<*>>, List<SecretAttribute>>? {
        try {
            val attributes =  content.lines()
                                     .map(String::trim)
                                     .filter { it.isNotEmpty() && !it.trimStart().startsWith("#") }
                                     .mapIndexed { i, line -> parseLine(line, i, hierarchicEnums, flatEnums) }

            val quasis: MutableList<QuasiAttribute<*>> = ArrayList(attributes.size)
            val secrets: MutableList<SecretAttribute> = ArrayList(attributes.size)

            attributes.map { it.fold(quasis::add, secrets::add) }

            return quasis to secrets
        } catch (ce: ConfigException) {
            log.warning("Unable to correctly parse attributes")
            log.warning(ce.message)
        }

        return null
    }

    /**
     * Parses single attribute config
     *
     * @param line the attribute descriptor line to parse
     * @param prevCount number of attributes already parsed (position of the attribute)
     * @param hierarchicEnums the list of (structured) hierarchic enums that are defined and can be referenced
     * @param flatEnums the list of flat flat enums that are defined and can be referenced
     */
    private fun parseLine(line: String, prevCount: Int, hierarchicEnums: List<NamedRecursiveBlock>, flatEnums: List<Pair<String, List<String>>>)
            : Either<QuasiAttribute<*>, SecretAttribute> {
        try {
            val tokens: MutableList<String> = LinkedList(line.trim().split("\\s+".toRegex()))
            val name = tokens.removeAt(0)
            val qualifier = tokens.removeAt(0)

            return when (qualifier) {
                "secret" -> Either.right(SecretAttribute(prevCount, name))
                "quasi" -> Either.left(parseQuasi(prevCount, name, tokens, hierarchicEnums, flatEnums))
                else -> throw ConfigException("Unrecognised attribute qualifier '$qualifier'")
            }

        } catch (ioe: IndexOutOfBoundsException) {
            throw ConfigException("Invalid line '$line', parameter missing")
        }
    }

    private fun parseQuasi(position: Int,
                           name: String,
                           tokens: MutableList<String>,
                           hierarchicEnums: List<NamedRecursiveBlock>,
                           flatEnums: List<Pair<String, List<String>>>): QuasiAttribute<*> {

        val type = tokens.removeAt(0)

        val typeParams = tokens.joinToString(prefix = "", separator = "", postfix = "")

        val parsed = when (type) {
            "Int"       -> parseInt(typeParams)
            "String"    -> parseString(typeParams)
            "Date"      -> parseDate(typeParams)
            else        -> {
                hierarchicEnums.find { it.name == type }?.let(::HierarchicAttribute) ?:
                flatEnums.find { it.first == type }?.let { EnumAttribute(it.second) } ?:
                throw ConfigException("unsupported type '$type'")
            }
        }
        return QuasiAttribute(position, name, parsed)
    }

    /**
     * Parses integer type column definition
     */
    private fun parseInt(params: String): IntAttribute =
            fromBrackets(params, parse = { it.toInt() },
                    missing = { IntAttribute() },
                    left = { IntAttribute(minValue = it) },
                    right = { IntAttribute(maxValue = it) },
                    both = { min, max -> IntAttribute(minValue = min, maxValue = max) })

    /**
     * Parses string type column definition
     */
    private fun parseString(params: String): StringAttribute =
            fromBrackets(params, parse = { it.toInt() },
                    missing = { StringAttribute() },
                    left = { StringAttribute(minLength = it) },
                    right = { StringAttribute(maxLength = it) },
                    both = { min, max -> StringAttribute(minLength =  min, maxLength = max) })

    /**
     * Parses date type column definition
     */
    private fun parseDate(params: String): DateAttribute {
        val tokens = params.split("[")
        val pattern = if (tokens.size != 2 || tokens[0].isEmpty()) "yyyy-MM-dd" else tokens[0]
        val formatter = DateTimeFormatter.ofPattern(pattern)

        return fromBrackets(params, parse = { LocalDate.parse(it, formatter) },
                missing = { DateAttribute() },
                left = { DateAttribute(formatterString = pattern, after = it) },
                right = { DateAttribute(formatterString = pattern, before = it) },
                both = { min, max -> DateAttribute(pattern, after = min, before = max) })
    }

    /**
     * Helper to parse range info
     *
     * @param params the value range an string
     * @param parse method to parse single value
     * @param missing producer if no value is present
     * @param left producer if only left value if present
     * @param right producer if only right value if present
     * @param both producer if string is a complete range
     */
    private fun <A : QuasiAttributeType<*>, T> fromBrackets(
            params: String, parse: (String) -> T?,
            missing: () -> A,
            left: (T) -> A,
            both: (T, T) -> A,
            right: (T) -> A): A {

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

    /**
     * Safely parses bracket range notation to optional left and right values (whichever is present)
     */
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