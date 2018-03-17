package hu.mktiti.kanon.attribute

class IntAttribute(
        private val minValue: Int = Int.MIN_VALUE,
        private val maxValue: Int = Int.MAX_VALUE) : AttributeType<Int>() {

    override fun parser(): (String) -> Int = {
        val value = it.toInt()
        if (value in minValue..maxValue) value
        else throw AttributeParseException("Int value [$value] out of range [$minValue;$maxValue]")
    }

    override fun toString() =
            "Int attribute [${if (minValue == Int.MIN_VALUE) "" else minValue.toString()};${if (maxValue == Int.MAX_VALUE) "" else maxValue.toString()}]"

    override fun isSame(a: Int, b: Int) = a == b

}