package com.k33.platform.filestore

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.k33.platform.utils.config.loadConfigEager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FileStoreService {

    suspend fun upload(
        bucketConfigId: String,
        filePath: String,
        contents: ByteArray,
    ) {
        val config = loadConfigEager<Config>(name = "gcs", path = "gcs.$bucketConfigId")
        val storage: Storage = StorageOptions.getDefaultInstance().service
        val blobId: BlobId = BlobId.of(config.bucketName, filePath)
        val blobInfo: BlobInfo = BlobInfo.newBuilder(blobId).build()
        withContext(Dispatchers.IO) {
            storage.create(blobInfo, contents)
        }
    }

    suspend fun download(
        bucketConfigId: String,
        filePath: String,
    ): ByteArray {
        val config = loadConfigEager<Config>(name = "gcs", path = "gcs.$bucketConfigId")
        val storage: Storage = StorageOptions.getDefaultInstance().service
        val blobId: BlobId = BlobId.of(config.bucketName, filePath)
        return withContext(Dispatchers.IO) {
            storage.readAllBytes(blobId)
        }
    }
}