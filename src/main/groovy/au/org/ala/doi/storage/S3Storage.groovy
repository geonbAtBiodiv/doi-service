package au.org.ala.doi.storage

import au.org.ala.doi.Doi
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.Headers
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.ObjectMetadata
import com.google.common.io.ByteSource
import grails.plugin.awssdk.s3.AmazonS3Service
import org.springframework.web.multipart.MultipartFile

class S3Storage extends BaseStorage {

    AmazonS3Service amazonS3Service

    S3Storage(AmazonS3Service amazonS3Service) {
        this.amazonS3Service = amazonS3Service
    }

    @Override
    ByteSource getFileForDoi(Doi doi) {

        def key = keyForDoi(doi)
        def exists = amazonS3Service.exists(key)
        if (exists) {
            def client = amazonS3Service.getClient()
            return new S3ByteSource(bucket: amazonS3Service.defaultBucketName, key: key, client: client)
        }
        return null
    }

    @Override
    void storeFileForDoi(Doi doi, MultipartFile incoming) {
        def filename = doi.filename ?: incoming.originalFilename ?: doi.uuid
        def key = "${doi.uuid}/${filename}"
        doi.filename = filename

        def url = amazonS3Service.storeMultipartFile(key, incoming, CannedAccessControlList.PublicRead)
        if (!url) {
            throw new IOException("Couldn't store $incoming.originalFilename in S3")
        }
    }

    @Override
    String getDescription() {
        return "Amazon S3 storage at ${amazonS3Service.client.endpoint ?: amazonS3Service.client.regionName ?: 'default S3'}, bucket: ${amazonS3Service.defaultBucketName}"
    }

    @Override
    void transferInputStream(Doi doi, InputStream input) {
        String key = "${doi.uuid}/${doi.filename}"
        ObjectMetadata om = new ObjectMetadata()
        om.contentType = doi.contentType
        om.setHeader(Headers.S3_CANNED_ACL, CannedAccessControlList.PublicRead.toString())

        def s3Url = amazonS3Service.storeInputStream(key, input, om)
        if (!s3Url) {
            throw new IOException("Couldn't store ${doi.uuid} in S3")
        }
    }

    String keyForDoi(Doi doi) {
        "${doi.uuid}/${doi.filename}"
    }

}

