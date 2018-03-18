package hu.mktiti.kanon.attribute
import java.time.LocalDateTime

import kotlin.reflect.KClass

/*
fun <T : Enum<*>> KClass<T>.asAttribute() = EnumAttribute(this.java.enumConstants.map { it.name }.toSet())

class EnumAttribute(
        private val valueSet: Set<String>) : AttributeType<String>() {

    override fun parser(): (String) -> String = {
        if (it in valueSet) it
        else throw AttributeParseException("Enum value [$it] not in set of possible values [$valueSet]")
    }

    override fun toString() = "One of $valueSet"

    override fun isSame(a: String, b: String) = a == b
}
        */