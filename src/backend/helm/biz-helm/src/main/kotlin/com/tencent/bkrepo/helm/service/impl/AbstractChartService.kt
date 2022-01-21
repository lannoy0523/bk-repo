/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.helm.service.impl

import com.tencent.bkrepo.common.api.util.readYamlString
import com.tencent.bkrepo.common.api.util.toYamlString
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.exception.RepoNotFoundException
import com.tencent.bkrepo.common.artifact.manager.StorageManager
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactService
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResourceWriter
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.service.exception.RemoteErrorCodeException
import com.tencent.bkrepo.helm.constants.CHART
import com.tencent.bkrepo.helm.constants.CHART_PACKAGE_FILE_EXTENSION
import com.tencent.bkrepo.helm.constants.FILE_TYPE
import com.tencent.bkrepo.helm.constants.FULL_PATH
import com.tencent.bkrepo.helm.constants.META_DETAIL
import com.tencent.bkrepo.helm.constants.NODE_CREATE_DATE
import com.tencent.bkrepo.helm.constants.NODE_FULL_PATH
import com.tencent.bkrepo.helm.constants.NODE_METADATA
import com.tencent.bkrepo.helm.constants.NODE_NAME
import com.tencent.bkrepo.helm.constants.NODE_SHA256
import com.tencent.bkrepo.helm.constants.OVERWRITE
import com.tencent.bkrepo.helm.constants.PROJECT_ID
import com.tencent.bkrepo.helm.constants.REPO_NAME
import com.tencent.bkrepo.helm.constants.REPO_TYPE
import com.tencent.bkrepo.helm.constants.SIZE
import com.tencent.bkrepo.helm.constants.TGZ_SUFFIX
import com.tencent.bkrepo.helm.exception.HelmBadRequestException
import com.tencent.bkrepo.helm.exception.HelmFileAlreadyExistsException
import com.tencent.bkrepo.helm.exception.HelmFileNotFoundException
import com.tencent.bkrepo.helm.exception.HelmRepoNotFoundException
import com.tencent.bkrepo.helm.pojo.artifact.HelmArtifactInfo
import com.tencent.bkrepo.helm.pojo.metadata.HelmChartMetadata
import com.tencent.bkrepo.helm.pojo.metadata.HelmIndexYamlMetadata
import com.tencent.bkrepo.helm.pool.HelmThreadPoolExecutor
import com.tencent.bkrepo.helm.utils.DecompressUtil.getArchivesContent
import com.tencent.bkrepo.helm.utils.HelmMetadataUtils
import com.tencent.bkrepo.helm.utils.HelmUtils
import com.tencent.bkrepo.helm.utils.ObjectBuilderUtil
import com.tencent.bkrepo.helm.utils.TimeFormatUtil
import com.tencent.bkrepo.repository.api.MetadataClient
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.request.PackagePopulateRequest
import com.tencent.bkrepo.repository.pojo.packages.request.PopulatedPackageVersion
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import com.tencent.bkrepo.repository.pojo.search.NodeQueryBuilder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ThreadPoolExecutor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher

// LateinitUsage: 抽象类中使用构造器注入会造成不便
@Suppress("LateinitUsage")
open class AbstractChartService : ArtifactService() {
    @Autowired
    lateinit var nodeClient: NodeClient

    @Autowired
    lateinit var metadataClient: MetadataClient

    @Autowired
    lateinit var repositoryClient: RepositoryClient

    @Autowired
    lateinit var packageClient: PackageClient

    @Autowired
    lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired
    lateinit var artifactResourceWriter: ArtifactResourceWriter

    @Autowired
    lateinit var storageManager: StorageManager

    val threadPoolExecutor: ThreadPoolExecutor = HelmThreadPoolExecutor.instance

    fun queryOriginalIndexYaml(): HelmIndexYamlMetadata {
        val context = ArtifactQueryContext()
        context.putAttribute(FULL_PATH, HelmUtils.getIndexCacheYamlFullPath())
        try {
            val inputStream = ArtifactContextHolder.getRepository().query(context) ?: throw HelmFileNotFoundException(
                "Error occurred when querying the index.yaml file.. "
            )
            return (inputStream as ArtifactInputStream).use { it.readYamlString() }
        } catch (e: Exception) {
            logger.error("Error occurred while querying index.yaml, error: ${e.message}")
            throw HelmFileNotFoundException(e.message.toString())
        }
    }

    /**
     * query original index.yaml file
     */
    fun getOriginalIndexYaml(projectId: String, repoName: String): HelmIndexYamlMetadata {
        val fullPath = HelmUtils.getIndexCacheYamlFullPath()
        val nodeDetail = nodeClient.getNodeDetail(projectId, repoName, fullPath).data
        val repository = repositoryClient.getRepoDetail(projectId, repoName, RepositoryType.HELM.name).data
            ?: throw RepoNotFoundException("Repository[$repoName] does not exist")
        val inputStream = storageManager.loadArtifactInputStream(nodeDetail, repository.storageCredentials)
            ?: throw HelmFileNotFoundException("Artifact[$fullPath] does not exist")
        return inputStream.use { it.readYamlString() }
    }

    /**
     * 下载index.yaml （local类型仓库index.yaml存储时使用的name时index-cache.yaml，remote需要转换）
     */
    fun downloadIndexYaml() {
        val context = ArtifactDownloadContext(null, ObjectBuilderUtil.buildIndexYamlRequest())
        context.putAttribute(FULL_PATH, HelmUtils.getIndexCacheYamlFullPath())
        try {
            ArtifactContextHolder.getRepository().download(context)
        } catch (e: Exception) {
            logger.error("Error occurred while downloading index.yaml, error: ${e.message}")
            throw HelmFileNotFoundException(e.message.toString())
        }
    }
    /**
     * upload index.yaml file
     */
    fun uploadIndexYamlMetadata(indexYamlMetadata: HelmIndexYamlMetadata) {
        val artifactFile = ArtifactFileFactory.build(indexYamlMetadata.toYamlString().byteInputStream())
        val context = ArtifactUploadContext(artifactFile)
        context.putAttribute(FULL_PATH, HelmUtils.getIndexCacheYamlFullPath())
        ArtifactContextHolder.getRepository().upload(context)
    }

    /**
     * upload index.yaml file
     */
    fun uploadIndexYamlMetadata(artifactFile: ArtifactFile, nodeCreateRequest: NodeCreateRequest) {
        val repository = repositoryClient.getRepoDetail(
            nodeCreateRequest.projectId,
            nodeCreateRequest.repoName,
            RepositoryType.HELM.name
        ).data
            ?: throw RepoNotFoundException("Repository[${nodeCreateRequest.repoName}] does not exist")
        storageManager.storeArtifactFile(nodeCreateRequest, artifactFile, repository.storageCredentials)
    }

    /**
     * 查询仓库相关信息
     */
    fun getRepositoryInfo(artifactInfo: ArtifactInfo): RepositoryDetail {
        with(artifactInfo) {
            val result = repositoryClient.getRepoDetail(projectId, repoName, REPO_TYPE).data ?: run {
                logger.error("check repository [$repoName] in projectId [$projectId] failed!")
                throw HelmRepoNotFoundException("repository [$repoName] in projectId [$projectId] not existed.")
            }
            return result
        }
    }

    /**
     * 根据路径取读取chart的Chart.yaml文件
     */
    fun queryHelmChartMetadata(context: ArtifactQueryContext, path: String): HelmChartMetadata {
        context.putAttribute(FULL_PATH, path)
        val artifactInputStream =
            ArtifactContextHolder.getRepository().query(context) as ArtifactInputStream
        context.putAttribute(SIZE, artifactInputStream.range.length)
        val content = artifactInputStream.use {
            it.getArchivesContent(CHART_PACKAGE_FILE_EXTENSION)
        }
        return content.byteInputStream().readYamlString()
    }

    /**
     * 查询仓库是否存在，以及仓库类型
     */
    fun checkRepositoryExistAndCategory(artifactInfo: ArtifactInfo) {
        with(artifactInfo) {
            val repo = repositoryClient.getRepoDetail(projectId, repoName, REPO_TYPE).data ?: run {
                logger.error("check repository [$repoName] in projectId [$projectId] failed!")
                throw HelmRepoNotFoundException("repository [$repoName] in projectId [$projectId] not existed.")
            }
            when (repo.category) {
                RepositoryCategory.REMOTE -> throw HelmBadRequestException(
                    "Unable to upload chart into a remote repository [$projectId/$repoName]"
                )
                else -> return
            }
        }
    }

    /**
     * 当helm 本地文件上传后，创建或更新包/包版本信息
     */
    fun initPackageInfo(context: ArtifactContext) {
        with(context) {
            if (CHART != getStringAttribute(FILE_TYPE)) return
            logger.info("start to update package meta info..")
            val size = getLongAttribute(SIZE)
            val helmChartMetadataMap = getAttribute<Map<String, Any>?>(META_DETAIL)
            helmChartMetadataMap?.let {
                val helmChartMetadata = HelmMetadataUtils.convertToObject(helmChartMetadataMap)
                val overWrite = getBooleanAttribute(OVERWRITE) ?: false
                createVersion(userId, artifactInfo, helmChartMetadata, size!!, overWrite)
            }
        }
    }

    /**
     * 查询节点
     */
    fun queryNodeList(
        artifactInfo: HelmArtifactInfo,
        exist: Boolean = true,
        lastModifyTime: LocalDateTime? = null
    ): List<Map<String, Any?>> {
        with(artifactInfo) {
            val queryModelBuilder = NodeQueryBuilder()
                .select(PROJECT_ID, REPO_NAME, NODE_NAME, NODE_FULL_PATH, NODE_METADATA, NODE_SHA256, NODE_CREATE_DATE)
                .sortByAsc(NODE_FULL_PATH)
                .page(PAGE_NUMBER, PAGE_SIZE)
                .projectId(projectId)
                .repoName(repoName)
                .fullPath(TGZ_SUFFIX, OperationType.SUFFIX)
            if (exist) {
                lastModifyTime?.let { queryModelBuilder.rule(true, NODE_CREATE_DATE, it, OperationType.AFTER) }
            }
            val result = nodeClient.search(queryModelBuilder.build()).data ?: run {
                logger.warn("don't find node list in repository: [$projectId/$repoName].")
                return emptyList()
            }
            return result.records
        }
    }

    /**
     * check node exists
     */
    fun exist(projectId: String, repoName: String, fullPath: String): Boolean {
        return nodeClient.checkExist(projectId, repoName, fullPath).data ?: false
    }

    /**
     * check package [key] version [version] exists
     */
    fun packageVersionExist(projectId: String, repoName: String, key: String, version: String): Boolean {
        return packageClient.findVersionByName(projectId, repoName, key, version).data?.let { true } ?: false
    }

    /**
     * check package [key] exists
     */
    fun packageExist(projectId: String, repoName: String, key: String): Boolean {
        return packageClient.findPackageByKey(projectId, repoName, key).data?.let { true } ?: false
    }

    /**
     * 发布事件
     */
    fun publishEvent(any: Any) {
        eventPublisher.publishEvent(any)
    }

    /**
     * 包版本数据填充
     * [nodeInfo] 某个节点的node节点信息
     */
    fun populatePackage(
        packageVersionList: List<PopulatedPackageVersion>,
        nodeInfo: NodeInfo,
        name: String,
        description: String
    ) {
        with(nodeInfo) {
            val packagePopulateRequest = PackagePopulateRequest(
                createdBy = nodeInfo.createdBy,
                createdDate = LocalDateTime.parse(createdDate),
                lastModifiedBy = nodeInfo.lastModifiedBy,
                lastModifiedDate = LocalDateTime.parse(lastModifiedDate),
                projectId = nodeInfo.projectId,
                repoName = nodeInfo.repoName,
                name = name,
                key = PackageKeys.ofHelm(name),
                type = PackageType.HELM,
                description = description,
                versionList = packageVersionList
            )
            packageClient.populatePackage(packagePopulateRequest)
        }
    }

    /**
     * 创建包版本
     */
    fun createVersion(
        userId: String,
        artifactInfo: ArtifactInfo,
        chartInfo: HelmChartMetadata,
        size: Long,
        isOverwrite: Boolean = false
    ) {
        val contentPath = HelmUtils.getChartFileFullPath(chartInfo.name, chartInfo.version)
        val packageVersionCreateRequest = ObjectBuilderUtil.buildPackageVersionCreateRequest(
            userId,
            artifactInfo,
            chartInfo,
            size,
            isOverwrite
        )
        val packageUpdateRequest = ObjectBuilderUtil.buildPackageUpdateRequest(artifactInfo, chartInfo)
        try {
            packageClient.createVersion(packageVersionCreateRequest).apply {
                logger.info("user: [$userId] create package version [$packageVersionCreateRequest] success!")
            }
            packageClient.updatePackage(packageUpdateRequest)
        } catch (exception: RemoteErrorCodeException) {
            // 暂时转换为包存在异常
            logger.warn("$contentPath already exists, message: ${exception.message}")
            throw HelmFileAlreadyExistsException("$contentPath already exists")
        }
    }

    /**
     * 下载index.yaml文件到本地存储
     */
    fun initIndexYaml(projectId: String, repoName: String) {
        logger.info("repo [$projectId/$repoName] has been created, will download index.yaml...")
        val repoDetail = repositoryClient.getRepoDetail(projectId, repoName, REPO_TYPE).data ?: run {
            logger.error("check repository [$repoName] in projectId [$projectId] failed!")
            throw HelmRepoNotFoundException("repository [$repoName] in projectId [$projectId] not existed.")
        }
        if (RepositoryCategory.REMOTE != repoDetail.category) {
            logger.warn("repo [$projectId/$repoName] does not need to download index.yaml")
            return
        }
        val fullPath = HelmUtils.getIndexCacheYamlFullPath()
        val context = ArtifactDownloadContext(repoDetail, ObjectBuilderUtil.buildIndexYamlRequest(projectId, repoName))
        nodeClient.deleteNode(
            NodeDeleteRequest(
                projectId,
                repoName,
                fullPath,
                context.userId
            )
        )
        context.putAttribute(FULL_PATH, fullPath)
        ArtifactContextHolder.getRepository().download(context)
    }

    companion object {
        const val PAGE_NUMBER = 0
        const val PAGE_SIZE = 100000
        val logger: Logger = LoggerFactory.getLogger(AbstractChartService::class.java)

        fun convertDateTime(timeStr: String): String {
            val localDateTime = LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_DATE_TIME)
            return TimeFormatUtil.convertToUtcTime(localDateTime)
        }
    }
}
