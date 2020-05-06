package com.tencent.bkrepo.npm.async

import com.google.gson.JsonObject
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.npm.constants.DEPENDENCIES
import com.tencent.bkrepo.npm.constants.DISTTAGS
import com.tencent.bkrepo.npm.constants.NAME
import com.tencent.bkrepo.npm.constants.VERSIONS
import com.tencent.bkrepo.npm.pojo.enums.NpmOperationAction
import com.tencent.bkrepo.repository.api.ModuleDepsResource
import com.tencent.bkrepo.repository.pojo.module.deps.service.DepsCreateRequest
import com.tencent.bkrepo.repository.pojo.module.deps.service.DepsDeleteRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class NpmDependentHandler {

    @Autowired
    private lateinit var moduleDepsResource: ModuleDepsResource

    @Async
    fun updatePkgDepts(userId: String, artifactInfo: ArtifactInfo, jsonObj: JsonObject, action: NpmOperationAction) {
        logger.info("updatePkgDependent current Thread : [${Thread.currentThread().name}]")

        val distTags = getDistTags(jsonObj)!!
        val versionJsonData = jsonObj.getAsJsonObject(VERSIONS).getAsJsonObject(distTags.second)

        when (action) {
            NpmOperationAction.PUBLISH -> {
                doDependentWithPublish(userId, artifactInfo, versionJsonData)
            }
            NpmOperationAction.UNPUBLISH -> {
                doDependentWithUnPublish(userId, artifactInfo, versionJsonData)
            }
            else -> {
                logger.warn("don't find operation action [${action.name}].")
            }
        }
    }

    private fun doDependentWithPublish(userId: String, artifactInfo: ArtifactInfo, versionJsonData: JsonObject) {
        val name = versionJsonData[NAME].asString
        if (versionJsonData.has(DEPENDENCIES)) {
            val dependenciesSet = versionJsonData.getAsJsonObject(DEPENDENCIES).keySet()
            val createList = mutableListOf<DepsCreateRequest>()
            if (dependenciesSet.isNotEmpty()) {
                dependenciesSet.forEach {
                    createList.add(
                        DepsCreateRequest(
                            projectId = artifactInfo.projectId,
                            repoName = artifactInfo.repoName,
                            name = it,
                            deps = name,
                            overwrite = true,
                            operator = userId
                        )
                    )
                }
            }
            moduleDepsResource.batchCreate(createList)
            logger.info("publish dependent for [$name] success.")
        }
    }

    private fun doDependentWithUnPublish(userId: String, artifactInfo: ArtifactInfo, versionJsonData: JsonObject) {
        val name = versionJsonData[NAME].asString
        moduleDepsResource.deleteAllByName(
            DepsDeleteRequest(
                projectId = artifactInfo.projectId,
                repoName = artifactInfo.repoName,
                deps = name,
                operator = userId
            )
        )
        logger.info("unPublish dependent for [$name] success.")
    }

    private fun getDistTags(jsonObj: JsonObject): Pair<String, String>? {
        val distTags = jsonObj.getAsJsonObject(DISTTAGS)
        distTags.entrySet().forEach {
            return Pair(it.key, it.value.asString)
        }
        return null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NpmDependentHandler::class.java)
    }
}
