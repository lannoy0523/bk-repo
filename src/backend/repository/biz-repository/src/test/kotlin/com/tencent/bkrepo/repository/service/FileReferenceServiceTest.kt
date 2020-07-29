package com.tencent.bkrepo.repository.service

import com.tencent.bkrepo.common.api.constant.StringPool.uniqueId
import com.tencent.bkrepo.repository.dao.FileReferenceDao
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import

@DisplayName("文件引用计数测试")
@DataMongoTest
@Import(
    FileReferenceDao::class
)
class FileReferenceServiceTest @Autowired constructor(
    private val fileReferenceService: FileReferenceService
) : ServiceBaseTest() {

    @MockBean
    private lateinit var repositoryService: RepositoryService

    @Test
    fun testIncrementAndDecrement() {
        val sha256 = uniqueId()
        Assertions.assertEquals(0, fileReferenceService.count(sha256, null))
        Assertions.assertTrue(fileReferenceService.increment(sha256, null))
        Assertions.assertEquals(1, fileReferenceService.count(sha256, null))
        Assertions.assertTrue(fileReferenceService.increment(sha256, null))
        Assertions.assertEquals(2, fileReferenceService.count(sha256, null))

        Assertions.assertTrue(fileReferenceService.decrement(sha256, null))
        Assertions.assertEquals(1, fileReferenceService.count(sha256, null))
        Assertions.assertTrue(fileReferenceService.decrement(sha256, null))
        Assertions.assertEquals(0, fileReferenceService.count(sha256, null))
        Assertions.assertFalse(fileReferenceService.decrement(sha256, null))
        Assertions.assertEquals(0, fileReferenceService.count(sha256, null))
    }
}
