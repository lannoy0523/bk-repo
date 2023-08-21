/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.job.batch.node

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.constant.LOG
import com.tencent.bkrepo.common.artifact.constant.REPORT
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.service.log.LoggerHolder
import com.tencent.bkrepo.job.DELETED_DATE
import com.tencent.bkrepo.job.FOLDER
import com.tencent.bkrepo.job.FULLPATH
import com.tencent.bkrepo.job.MEMORY_CACHE_TYPE
import com.tencent.bkrepo.job.PROJECT
import com.tencent.bkrepo.job.REDIS_CACHE_TYPE
import com.tencent.bkrepo.job.REPO
import com.tencent.bkrepo.job.batch.base.ChildJobContext
import com.tencent.bkrepo.job.batch.base.ChildMongoDbBatchJob
import com.tencent.bkrepo.job.batch.base.JobContext
import com.tencent.bkrepo.job.batch.context.FolderChildContext
import com.tencent.bkrepo.job.config.properties.CompositeJobProperties
import com.tencent.bkrepo.job.config.properties.NodeStatCompositeMongoDbBatchJobProperties
import org.springframework.data.mongodb.core.BulkOperations.BulkMode
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.RedisTemplate
import java.time.DayOfWeek
import java.time.LocalDateTime
import kotlin.text.toLongOrNull as toLongOrNull1


/**
 * 目录大小以及文件个数统计
 */
class FolderStatChildJob(
    val properties: CompositeJobProperties,
    private val mongoTemplate: MongoTemplate,
    private val redisTemplate: RedisTemplate<String, String>
) : ChildMongoDbBatchJob<NodeStatCompositeMongoDbBatchJob.Node>(properties) {


    override fun onParentJobStart(context: ChildJobContext) {
        require(context is FolderChildContext)
        initCheck(context)
        logger.info("start to stat the size of folder, init flag is ${context.initFlag}")
    }

    override fun run(row: NodeStatCompositeMongoDbBatchJob.Node, collectionName: String, context: JobContext) {
        require(context is FolderChildContext)
        if (context.initFlag) return
        if (row.deleted != null) return
        // 判断是否在不统计项目或者仓库列表中
        if (ignoreProjectOrRepoCheck(row.projectId, row.repoName)) return
        //只统计非目录类节点；没有根目录这个节点，不需要统计
        if (row.folder || row.path == PathUtils.ROOT) {
            return
        }

        // 更新当前节点所有上级目录（排除根目录）统计信息
        val folderFullPaths = PathUtils.resolveAncestorFolder(row.fullPath)
        for (fullPath in folderFullPaths) {
            if (fullPath == PathUtils.ROOT) continue
            updateCache(
                collectionName = collectionName,
                projectId = row.projectId,
                repoName = row.repoName,
                fullPath = fullPath,
                size = row.size,
                context = context
            )
        }
    }

    override fun onParentJobFinished(context: ChildJobContext) {
        require(context is FolderChildContext)
        logger.info("stat size of folder done")
    }


    override fun createChildJobContext(parentJobContext: JobContext): ChildJobContext {
        val cacheType = try {
            redisTemplate.execute { null }
            REDIS_CACHE_TYPE
        } catch (e: Exception) {
            MEMORY_CACHE_TYPE
        }
        return FolderChildContext(parentJobContext, cacheType = cacheType)
    }

    override fun onRunCollectionFinished(collectionName: String, context: JobContext) {
        super.onRunCollectionFinished(collectionName, context)
        require(context is FolderChildContext)
        // 当表执行完成后，将属于该表的所有记录写入数据库
        storeCacheToDB(collectionName, context)
        context.projectMap[collectionName] = mutableSetOf()
    }

    /**
     * 判断项目或者仓库是否不需要进行目录统计
     */
    private fun ignoreProjectOrRepoCheck(projectId: String, repoName: String): Boolean {
        return IGNORE_PROJECT_PREFIX_LIST.firstOrNull { projectId.startsWith(it) } != null
            || IGNORE_REPO_LIST.contains(repoName)
    }

    /**
     * 更新缓存中的size和nodeNum
     */
    private fun updateCache(
        collectionName: String,
        projectId: String,
        repoName: String,
        fullPath: String,
        size: Long,
        context: FolderChildContext
    ) {
        if (context.cacheType == REDIS_CACHE_TYPE) {
            updateRedisCache(
                collectionName = collectionName,
                projectId = projectId,
                repoName = repoName,
                fullPath = fullPath,
                size = size
            )
        } else {
            updateMemoryCache(
                collectionName = collectionName,
                projectId = projectId,
                repoName = repoName,
                fullPath = fullPath,
                size = size,
                context = context
            )
        }
        context.projectMap.putIfAbsent(collectionName, mutableSetOf())
        context.projectMap[collectionName]!!.add(projectId)
    }

    /**
     * 更新redis缓存中对应key下将新增的size和nodeNum
     */
    private fun updateRedisCache(
        collectionName: String,
        projectId: String,
        repoName: String,
        fullPath: String,
        size: Long
    ) {
        val sizeHKey = buildCacheKey(projectId = projectId, repoName = repoName, fullPath = fullPath, tag = SIZE)
        val nodeNumHKey = buildCacheKey(projectId = projectId, repoName = repoName, fullPath = fullPath, tag = NODE_NUM)
        val key = buildCacheKey(collectionName = collectionName, projectId = projectId)
        val hashOps = redisTemplate.opsForHash<String, Long>()
        hashOps.increment(key, sizeHKey, size)
        hashOps.increment(key, nodeNumHKey, 1)
    }

    /**
     * 更新内存缓存中对应key下将新增的size和nodeNum
     */
    private fun updateMemoryCache(
        collectionName: String,
        projectId: String,
        repoName: String,
        fullPath: String,
        size: Long,
        context: FolderChildContext
    ) {
        val key = buildCacheKey(
            collectionName = collectionName, projectId = projectId, repoName = repoName, fullPath = fullPath
        )
        val folderMetrics = context.folderCache.getOrPut(key) { FolderChildContext.FolderMetrics() }
        folderMetrics.capSize.add(size)
        folderMetrics.nodeNum.increment()
    }

    private fun initCheck(context: FolderChildContext) {
        require(properties is NodeStatCompositeMongoDbBatchJobProperties)
        if (LocalDateTime.now().dayOfWeek != DayOfWeek.of(properties.dayOfWeek)) {
            return
        }
        context.initFlag = false
    }

    /**
     * 将缓存中的数据更新到DB中
     */
    private fun storeCacheToDB(collectionName: String, context: FolderChildContext) {
        if (context.cacheType == REDIS_CACHE_TYPE) {
            storeRedisCacheToDB(collectionName, context)
        } else {
            storeMemoryCacheToDB(collectionName, context)
        }
    }


    /**
     * 将redis缓存中属于collectionName下的记录写入DB中
     */
    private fun storeRedisCacheToDB(collectionName: String, context: FolderChildContext) {
        val hashOps = redisTemplate.opsForHash<String, String>()
        context.projectMap[collectionName]?.forEach {

            val projectKey = buildCacheKey(collectionName = collectionName, projectId = it)
            val storedProjectIdKey = buildCacheKey(collectionName = collectionName, projectId = it, tag = STORED)

            val updateList = ArrayList<org.springframework.data.util.Pair<Query, Update>>()

            for (entry in hashOps.entries(projectKey).entries) {
                val folderInfo = extractFolderInfoFromRedisKey(entry.key) ?: continue
                // 由于可能KEYS或者SCAN命令会被禁用，调整redis存储格式，key为collectionName,
                // hkey为projectId:repoName:fullPath:size或者nodenum, hvalue为对应值,
                // 为了避免遍历时删除，用一个额外的key去记录当前collectionName下已经存储到db的目录记录
                val storedFolderHkey = buildCacheKey(
                    projectId = folderInfo.projectId, repoName = folderInfo.repoName, fullPath = folderInfo.fullPath
                )
                if (redisTemplate.opsForHash<String,String>().get(storedProjectIdKey, storedFolderHkey) == null) {
                    val statInfo = getFolderStatInfo(
                        collectionName, entry, folderInfo, hashOps
                    )
                    updateList.add(buildUpdateClausesForFolder(
                        projectId = folderInfo.projectId,
                        repoName = folderInfo.repoName,
                        fullPath = folderInfo.fullPath,
                        size = statInfo.size,
                        nodeNum = statInfo.nodeNum
                    ))
                    if (updateList.size >= BATCH_LIMIT) {
                        mongoTemplate.bulkOps(BulkMode.UNORDERED,collectionName)
                            .updateOne(updateList)
                            .execute()
                        updateList.clear()
                        try {
                            Thread.sleep(500)
                        } catch (_: Exception) {
                        }
                    }
                    redisTemplate.opsForHash<String, String>().put(storedProjectIdKey, storedFolderHkey, STORED)
                }
            }
            redisTemplate.delete(projectKey)
            redisTemplate.delete(storedProjectIdKey)
        }
    }


    /**
     * 从redis中获取对应目录的统计信息
     */
    private fun getFolderStatInfo(
        collectionName: String,
        entry: MutableMap.MutableEntry<String, String>,
        folderInfo: FolderInfo,
        hashOps: HashOperations<String, String, String>
    ): StatInfo {
        val size: Long
        val nodeNum: Long
        val key = buildCacheKey(collectionName = collectionName, projectId = folderInfo.projectId)
        if (entry.key.endsWith(SIZE)) {
            val nodeNumKey = buildCacheKey(
                projectId = folderInfo.projectId, repoName = folderInfo.repoName,
                fullPath = folderInfo.fullPath, tag = NODE_NUM
            )
            size = entry.value.toLongOrNull1() ?: 0
            nodeNum = hashOps.get(key, nodeNumKey)?.toLongOrNull1() ?: 0
        } else {
            val sizeKey = buildCacheKey(
                projectId = folderInfo.projectId, repoName = folderInfo.repoName,
                fullPath = folderInfo.fullPath, tag = SIZE
            )
            nodeNum = entry.value.toLongOrNull1() ?: 0
            size = hashOps.get(key, sizeKey)?.toLongOrNull1() ?: 0
        }
        return StatInfo(size, nodeNum)
    }

    /**
     * 将memory缓存中属于collectionName下的记录写入DB中
     */
    private fun storeMemoryCacheToDB(collectionName: String, context: FolderChildContext) {
        if (context.folderCache.isEmpty()) {
            return
        }
        val updateList = ArrayList<org.springframework.data.util.Pair<Query, Update>>()

        val prefix = buildCacheKey(collectionName = collectionName, projectId = StringPool.EMPTY)
        context.folderCache.filterKeys { it.startsWith(prefix) }.forEach {  entry ->
            extractFolderInfoFromCacheKey(entry.key)?.let {
                updateList.add(buildUpdateClausesForFolder(
                    projectId = it.projectId,
                    repoName = it.repoName,
                    fullPath = it.fullPath,
                    size = entry.value.capSize.toLong(),
                    nodeNum = entry.value.nodeNum.toLong()
                ))
                if (updateList.size >= BATCH_LIMIT) {
                    mongoTemplate.bulkOps(BulkMode.UNORDERED,collectionName)
                        .updateOne(updateList)
                        .execute()
                    updateList.clear()
                    try {
                        Thread.sleep(500)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    /**
     * 生成db更新语句
     */
    private fun buildUpdateClausesForFolder(
        projectId: String,
        repoName: String,
        fullPath: String,
        size: Long,
        nodeNum: Long
    ): org.springframework.data.util.Pair<Query, Update> {
        val query = Query(
            Criteria.where(PROJECT).isEqualTo(projectId)
                .and(REPO).isEqualTo(repoName)
                .and(FULLPATH).isEqualTo(fullPath)
                .and(DELETED_DATE).isEqualTo(null)
                .and(FOLDER).isEqualTo(true)
        )
        val update = Update().set(SIZE, size)
            .set(NODE_NUM, nodeNum)
        return org.springframework.data.util.Pair.of(query, update)
    }


    /**
     * 生成缓存key
     */
    private fun buildCacheKey(
        projectId: String,
        repoName: String? = null,
        fullPath: String? = null,
        collectionName: String? = null,
        tag: String? = null,
    ): String {
        return StringBuilder().apply {
            collectionName?.let {
                this.append(it).append(StringPool.COLON)
            }
            this.append(projectId)
            repoName?.let {
                this.append(StringPool.COLON).append(repoName)
            }
            fullPath?.let {
                this.append(StringPool.COLON).append(fullPath)
            }
            tag?.let {
                this.append(StringPool.COLON).append(tag)
            }
        }.toString()
    }

    private fun extractFolderInfoFromRedisKey(key: String): FolderInfo? {
        val values = key.split(StringPool.COLON)
        return try {
            FolderInfo(
                projectId = values[0],
                repoName = values[1],
                fullPath = values[2]
            )
        } catch (e: Exception) {
            null
        }
    }



    /**
     * 从缓存key中解析出目录信息
     */
    private fun extractFolderInfoFromCacheKey(key: String): FolderInfo? {
        val values = key.split(StringPool.COLON)
        return try {
            FolderInfo(
                projectId = values[1],
                repoName = values[2],
                fullPath = values[3]
            )
        } catch (e: Exception) {
            null
        }
    }



    data class FolderInfo(
        var projectId: String,
        var repoName: String,
        var fullPath: String
    )

    data class StatInfo(
        var size: Long,
        var nodeNum: Long
    )


    companion object {
        private val logger = LoggerHolder.jobLogger
        private const val SIZE = "size"
        private const val NODE_NUM = "nodeNum"
        private val IGNORE_PROJECT_PREFIX_LIST = listOf("CODE_", "CLOSED_SOURCE_", "git_")
        private val IGNORE_REPO_LIST = listOf(REPORT, LOG)
        private const val STORED = "stored"
        private const val BATCH_LIMIT = 500
    }
}
