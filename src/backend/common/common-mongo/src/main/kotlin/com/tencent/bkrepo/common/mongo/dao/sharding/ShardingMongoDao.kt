package com.tencent.bkrepo.common.mongo.dao.sharding

import com.mongodb.BasicDBList
import com.tencent.bkrepo.common.mongo.dao.AbstractMongoDao
import com.tencent.bkrepo.common.mongo.dao.util.MongoIndexResolver
import com.tencent.bkrepo.common.mongo.dao.util.ShardingUtils
import org.apache.commons.lang3.reflect.FieldUtils
import org.apache.commons.lang3.reflect.FieldUtils.getFieldsListWithAnnotation
import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.index.IndexDefinition
import org.springframework.data.mongodb.core.query.Query
import java.lang.reflect.Field
import javax.annotation.PostConstruct

/**
 * mongodb 支持分表的数据访问层抽象类
 */
abstract class ShardingMongoDao<E> : AbstractMongoDao<E>() {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    /**
     * 分表Field
     */
    private val shardingField: Field
    /**
     * 分表列名
     */
    private val shardingColumn: String
    /**
     * 分表数
     */
    private val shardingCount: Int

    init {
        @Suppress("LeakingThis")
        val fieldsWithShardingKey = getFieldsListWithAnnotation(classType, ShardingKey::class.java)
        require(fieldsWithShardingKey.size == 1) {
            "Only one field could be annotated with ShardingKey annotation but find ${fieldsWithShardingKey.size}!"
        }

        this.shardingField = fieldsWithShardingKey[0]
        this.shardingColumn = determineShardingColumn()

        val shardingKey = AnnotationUtils.getAnnotation(shardingField, ShardingKey::class.java)!!
        this.shardingCount = ShardingUtils.shardingCountFor(shardingKey.count)
    }

    @PostConstruct
    private fun init() {
        ensureIndex()
    }

    private fun ensureIndex() {
        val start = System.currentTimeMillis()
        val indexDefinitions = MongoIndexResolver.resolveIndexFor(classType)
        val nonexistentIndexDefinitions = filterExistedIndex(indexDefinitions)
        nonexistentIndexDefinitions.forEach {
            for (i in 1..shardingCount) {
                val mongoTemplate = determineMongoTemplate()
                val collectionName = parseSequenceToCollectionName(i - 1)
                mongoTemplate.indexOps(collectionName).ensureIndex(it)
            }
        }

        val indexCount = shardingCount * indexDefinitions.size
        val consume = System.currentTimeMillis() - start

        logger.info("Ensure [$indexCount] index for sharding collection [$collectionName], consume [$consume] ms totally")
    }

    private fun filterExistedIndex(indexDefinitions: List<IndexDefinition>): List<IndexDefinition> {
        val mongoTemplate = determineMongoTemplate()
        val collectionName = parseSequenceToCollectionName(0)
        val indexInfoList = mongoTemplate.indexOps(collectionName).indexInfo
        val indexNameList = indexInfoList.map { index -> index.name }
        return indexDefinitions.filter { index ->
            val indexOptions = index.indexOptions
            if (indexOptions.contains("name")) {
                val indexName = indexOptions.getString("name")
                !indexNameList.contains(indexName)
            } else true
        }
    }

    private fun shardingKeyToCollectionName(shardValue: Any): String {
        val shardingSequence = ShardingUtils.shardingSequenceFor(shardValue, shardingCount)
        return parseSequenceToCollectionName(shardingSequence)
    }

    fun parseSequenceToCollectionName(sequence: Int): String {
        return collectionName + "_" + sequence
    }

    private fun determineShardingColumn(): String {
        val shardingKey = AnnotationUtils.getAnnotation(shardingField, ShardingKey::class.java)!!
        if (shardingKey.column.isNotEmpty()) {
            return shardingKey.column
        }
        val fieldAnnotation = AnnotationUtils.getAnnotation(shardingField, org.springframework.data.mongodb.core.mapping.Field::class.java)
        if (fieldAnnotation != null && fieldAnnotation.value.isNotEmpty()) {
            return fieldAnnotation.value
        }
        return shardingField.name
    }

    override fun determineCollectionName(): String {
        if (classType.isAnnotationPresent(ShardingDocument::class.java)) {
            val document = classType.getAnnotation(ShardingDocument::class.java)
            return document.collection
        }
        return super.determineCollectionName()
    }

    override fun determineMongoTemplate(): MongoTemplate {
        return this.mongoTemplate
    }

    override fun determineCollectionName(entity: E): String {
        val shardingValue = FieldUtils.readField(shardingField, entity, true)
        requireNotNull(shardingValue) { "Sharding value can not be empty !" }

        return shardingKeyToCollectionName(shardingValue)
    }

    override fun determineCollectionName(query: Query): String {
        val shardingValue = determineCollectionName(query.queryObject)
        requireNotNull(shardingValue) { "Sharding value can not empty !" }

        return shardingKeyToCollectionName(shardingValue)
    }

    override fun determineCollectionName(aggregation: Aggregation): String {
        var shardingValue: Any? = null
        val pipeline = aggregation.toPipeline(Aggregation.DEFAULT_CONTEXT)
        for (document in pipeline) {
            if (document.containsKey("\$match")) {
                val subDocument = document["\$match"] as Document
                shardingValue = subDocument["projectId"]
                break
            }
        }

        requireNotNull(shardingValue) { "sharding value can not be empty!" }
        return shardingKeyToCollectionName(shardingValue)
    }

    private fun determineCollectionName(document: Document): Any? {
        for ((key, value) in document) {
            if (key == shardingColumn) return value
            if (key == "\$and") {
                for (element in value as BasicDBList) {
                    determineCollectionName(element as Document)?.let { return it }
                }
            }
        }
        return null
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ShardingMongoDao::class.java)
    }
}
