package com.tencent.bkrepo.pypi.pojo

data class Basic(
    val name: String,
    val version: String,
    val size: Long,
    val fullPath: String,
    val createdBy: String,
    val createdDate: String,
    val lastModifiedBy: String,
    val lastModifiedDate: String,
    val downloadCount: Long,
    val sha256: String?,
    val md5: String?,
    val stageTag: List<String>?,
    val description: String?
)