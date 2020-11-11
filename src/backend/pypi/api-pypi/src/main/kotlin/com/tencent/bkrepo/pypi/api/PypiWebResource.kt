package com.tencent.bkrepo.pypi.api

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.pypi.artifact.PypiArtifactInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Api("pypi 产品-web接口")
@RequestMapping("/ext")
interface PypiWebResource {

    @ApiOperation("pypi 包删除接口")
    @DeleteMapping(PypiArtifactInfo.PYPI_EXT_PACKAGE_DELETE)
    fun deletePackage(
        @ArtifactPathVariable pypiArtifactInfo: PypiArtifactInfo,
        @RequestParam packageKey: String
    ): Response<Void>

    @ApiOperation("pypi 版本删除接口")
    @DeleteMapping(PypiArtifactInfo.PYPI_EXT_VERSION_DELETE)
    fun deleteVersion(
        @ArtifactPathVariable pypiArtifactInfo: PypiArtifactInfo,
        @RequestParam packageKey: String,
        @RequestParam version: String?
    ): Response<Void>

    @ApiOperation("pypi 版本详情接口")
    @GetMapping(PypiArtifactInfo.PYPI_EXT_DETAIL)
    fun artifactDetail(
        @ArtifactPathVariable pypiArtifactInfo: PypiArtifactInfo,
        @RequestParam packageKey: String,
        @RequestParam version: String?
    ): Response<Any?>
}