package hu.mktiti.kanon.attribute

class StringAttribute(
        private val minLength: Int = 0,
        private val maxLength: Int = Int.MAX_VALUE) : AttributeType<String>() {

    override fun parser(): (String) -> String = {
        if (it.length in minLength..maxLength) it
        else throw AttributeParseException("String value [$it] out of length range [$minLength;$maxLength]")
    }

    override fun toString() =
            "String attribute (length: [$minLength;${if (maxLength == Int.MAX_VALUE) "" else maxLength.toString()}])"

    override fun isSame(a: String, b: String) = a == b

}