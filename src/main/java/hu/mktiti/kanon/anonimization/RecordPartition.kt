package hu.mktiti.kanon.anonimization

import hu.mktiti.kanon.attribute.AttributeValue
import hu.mktiti.kanon.attribute.Partition
import hu.mktiti.kanon.attribute.QuasiAttribute
import hu.mktiti.kanon.attribute.RecordDescriptor
import hu.mktiti.kanon.logger
import java.io.PrintStream

class RecordPartition(private val descriptor: RecordDescriptor, private val kValue: Int, private val storeLimit: Int, records: List<Record>) {
    private val log by logger()

    private val records: MutableList<Record> = ArrayList<Record>(2 * storeLimit).apply { addAll(records) }

    fun recordsView(): List<Record> = records

    val size: Int
        get() = records.size

    val isEmpty: Boolean
        get() = records.isEmpty()

    private var prevAttributePartitions: Map<QuasiAttribute<*>, AttributeValue>? = null
    private var attributePartitions: Map<QuasiAttribute<*>, Partition<*>> = calculateAttribPartitions()

    private fun recordView(position: Int) = records.map { it[position] }

    private fun recordView(attribute: QuasiAttribute<*>) = recordView(attribute.position)

    private fun copy(records: List<Record>) = RecordPartition(descriptor, kValue, storeLimit, records)

    fun clear() {
        prevAttributePartitions = attributePartitions.mapValues { it.value.aggregateValue }
        records.clear()
        attributePartitions = calculateAttribPartitions()
    }

    fun errorSum() = attributePartitions.entries.sumByDouble { (k, v) -> k.type.partitionError(v as Partition<Nothing>) }

    private fun attribMapContains(record: Record) = attributePartitions.asSequence().all { (attribute, partition) ->
        attribute.type.unsafeSubsetOf(partition.aggregateValue, record[attribute.position])
    }

    private fun prevAttribsContains(record: Record) = prevAttributePartitions?.let {
        attributePartitions.asSequence().all { (attribute, aggregate) ->
            attribute.type.unsafeSubsetOf(aggregate, record[attribute.position])
        }
    } ?: true

    fun contains(record: Record): Boolean = attribMapContains(record) && prevAttribsContains(record)

    private fun calculateAttribPartitions() = descriptor.quasiAttributes.associate { attribute ->
        attribute to attribute.type.unsafePartition(recordView(attribute))
    }

    fun add(record: Record): Boolean {
        records.add(record)
        attributePartitions = if (size == 1) {
            descriptor.quasiAttributes.associate {
                attribute -> attribute to attribute.type.unsafeSingletonPartition(record[attribute.position])
            }
        } else {
            calculateAttribPartitions()
        }
        return size > kValue
    }

    fun split(): Triple<RecordPartition, RecordPartition, Double>? {
        if (size < 2 * kValue) return null

        val bestSplit = descriptor.quasiAttributes.map { attrib ->

            val partition = attrib.type.partition(recordView(attrib) as List<Nothing>) as Partition<Nothing>
            val split = attrib.type.split(partition, kValue)
            attrib to (split ?: return@map null)

        }.filterNotNull().maxBy {
            it.second.informationGainValue
        }

        if (bestSplit == null) {
            log.info("Cannot split partition. This may indicate an error in the program")
        } else {
            val (partA, partB) = records.partition { record ->
                val attribValue = record[bestSplit.first.position]
                bestSplit.second.partitionA.values.any { it === attribValue }
            }

            return Triple(copy(partA), copy(partB), bestSplit.second.informationGainValue)
        }

        return null
    }

    fun splitRecursively(): List<RecordPartition> =
        split()?.let { (partA, partB, _) ->
            partA.splitRecursively() + partB.splitRecursively()
        } ?: listOf(this)

    fun releaseAll(outStream: PrintStream) {
        val staticAttribs = mutableListOf<Pair<Int, String>>()
        staticAttribs.addAll(attributePartitions.map { (attrib, part) -> attrib.position to attrib.type.showUnsafe(part.aggregateValue) })
        staticAttribs.addAll(descriptor.secretAttributes.map { it.position to "*" })

        for (record in records) {
            val allAttribs: MutableList<Pair<Int, String>> = staticAttribs.toMutableList()
            allAttribs.addAll(descriptor.passthroughAttributes.map { it.position to record[it.position] as String })
            allAttribs.addAll(descriptor.secretIdentityAttributes.map { it.position to it.convert(record[it.position].toString()) })
            allAttribs.sortBy { it.first }
            outStream.print(allAttribs.joinToString(prefix = "", separator = ";", postfix = "\n") { it.second })
        }
    }

}