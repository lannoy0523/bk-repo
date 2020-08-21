package com.tencent.bkrepo.common.storage.innercos

import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.storage.core.AbstractFileStorage
import com.tencent.bkrepo.common.storage.credentials.InnerCosCredentials
import com.tencent.bkrepo.common.storage.innercos.client.CosClient
import com.tencent.bkrepo.common.storage.innercos.request.CheckObjectExistRequest
import com.tencent.bkrepo.common.storage.innercos.request.CopyObjectRequest
import com.tencent.bkrepo.common.storage.innercos.request.DeleteObjectRequest
import com.tencent.bkrepo.common.storage.innercos.request.GetObjectRequest
import java.io.File
import java.io.InputStream

/**
 * 内部cos文件存储实现类
 */
open class InnerCosFileStorage : AbstractFileStorage<InnerCosCredentials, CosClient>() {

    override fun store(path: String, filename: String, file: File, client: CosClient) {
        client.putFileObject(filename, file)
    }

    override fun store(path: String, filename: String, inputStream: InputStream, size: Long, client: CosClient) {
        client.putStreamObject(filename, inputStream, size)
    }

    override fun load(path: String, filename: String, range: Range, client: CosClient): InputStream? {
        val request = GetObjectRequest(filename, range.start, range.end)
        return client.getObject(request).inputStream
    }

    override fun delete(path: String, filename: String, client: CosClient) {
        return try {
            client.deleteObject(DeleteObjectRequest(filename))
        } catch (ignored: Exception) {
        }
    }

    override fun exist(path: String, filename: String, client: CosClient): Boolean {
        return try {
            return client.checkObjectExist(CheckObjectExistRequest(filename))
        } catch (ignored: Exception) {
            false
        }
    }

    override fun copy(path: String, filename: String, fromClient: CosClient, toClient: CosClient) {
        try {
            require(fromClient.credentials.region == toClient.credentials.bucket)
            require(fromClient.credentials.secretId == toClient.credentials.secretId)
            require(fromClient.credentials.secretKey == toClient.credentials.secretKey)
        } catch (exception: IllegalArgumentException) {
            throw IllegalArgumentException("Unsupported to copy object between different cos app id")
        }
        toClient.copyObject(CopyObjectRequest(fromClient.credentials.bucket, filename, filename))
    }

    override fun onCreateClient(credentials: InnerCosCredentials): CosClient {
        require(credentials.secretId.isNotBlank())
        require(credentials.secretKey.isNotBlank())
        require(credentials.region.isNotBlank())
        require(credentials.bucket.isNotBlank())
        return CosClient(credentials)
    }

}
