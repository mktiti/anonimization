package hu.mktiti.kanon

import java.util.*
import java.util.logging.Logger
import kotlin.reflect.full.companionObject

/**
 * logger helper for any object/class
 * lazily initialized
 */
fun <R : Any> R.logger(): Lazy<Logger> {
    return lazy { Logger.getLogger(unwrapCompanionClass(this.javaClass).name) }
}

/**
 * helper for logger
 */
fun <T: Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> {
    return if (ofClass.enclosingClass != null && ofClass.enclosingClass.kotlin.companionObject?.java == ofClass) {
        ofClass.enclosingClass
    } else {
        ofClass
    }
}

/**
 * Splits String at given indices
 */
fun splitAtAll(s: String, indices: List<Int>): List<String> {
    if (s.isEmpty()) return listOf(s)

    val postIndices = LinkedList<Int>()
    postIndices.addAll(indices)
    postIndices.add(s.length)

    val list = LinkedList<String>()
    var prev = 0
    for (i in postIndices) {
        list.add(s.substring(prev, i))
        prev = i
    }

    return list
}

/**
 * Safely convert ot non-null value
 *
 * @param producers unsafe producers to use in the given oder to create value, if one returns non-null value that value is used
 * @param default default, safe producer. used if all other producers fail
 */
fun <T> coalesce(vararg producers: () -> T?, default: () -> T): T =
    producers.asSequence().map { it() }.filter { it != null }.first() ?: default()

/**
 * Block with name and list of generic content
 */
open class NamedBlock<out T>(val name: String, val content: List<T>)

fun splitNamedBlock(content: String): Pair<String, String?> {
    var cleaned = content.trim()

    if (cleaned.lastOrNull() ?: ' ' != '}') {
        if (cleaned.contains('{') || cleaned.isBlank())
            throw IllegalArgumentException("Invalid block '$content'")
        else
            return cleaned to null
    }
    cleaned = cleaned.dropLast(1)

    val opening = cleaned.indexOf('{')
    if (opening == -1) throw IllegalArgumentException("Invalid block '$content'")

    val name = cleaned.subSequence(startIndex = 0, endIndex = opening).trim().toString()
    if (name.isBlank()) throw IllegalArgumentException("Invalid block '$content'")
    if (opening == cleaned.length - 1) return name to null

    return name to cleaned.subSequence(opening + 1, cleaned.length).trim().toString()
}

fun <T> parseNamedBlock(string: String, parser: (String) -> List<T>?): NamedBlock<T>? {
    val (name, content) = splitNamedBlock(string)
    return NamedBlock(name, content?.let(parser) ?: emptyList())
}

fun parseRecursiveNamedBlock(string: String, parser: (String) -> List<NamedRecursiveBlock> = ::parseContentList): NamedRecursiveBlock {
    val (name, content) = splitNamedBlock(string)
    return NamedRecursiveBlock(name, content?.let(parser) ?: emptyList())
}

fun parseContentList(s: String): List<NamedRecursiveBlock> {
    val helper = s.map {
        when (it) {
            '{' -> 1
            '}' -> -1
            else -> 0
        }
    }

    val array = IntArray(s.length, {0})
    var currLevel = 0
    s.indices.forEach {
        currLevel += helper[it]
        array[it] = currLevel
        currLevel += helper[it]
    }

    val indices = s.mapIndexed { i, c ->
        if (c == ',' || c == '\n') i else null
    }.filter {
        it != null && array[it] == 0
    }.requireNoNulls()

    return splitAtAll(s, indices)
            .map { it.removePrefix(",") }
            .filter(String::isNotBlank)
            .map { parseRecursiveNamedBlock(it) }
}