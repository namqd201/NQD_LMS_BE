package com.nqd.nqd_lms_be.service.r2;

import java.io.InputStream;

public interface R2StorageService {

    /**
     * Upload an input stream directly to Cloudflare R2.
     *
     * @param key S3 object key (e.g. "recordings/class-123/buoi-1.mp4")
     * @param inputStream Data stream
     * @param contentLength Length of stream in bytes
     * @param contentType MIME type (e.g. "video/mp4")
     * @return Public accessible URL of the uploaded file
     */
    String uploadStream(String key, InputStream inputStream, long contentLength, String contentType);

    /**
     * Stream a remote file (e.g. from Lark recording download URL) directly to Cloudflare R2
     * without saving to local disk.
     *
     * @param sourceUrl Remote source URL (e.g. Lark recording URL)
     * @param targetKey Destination S3 key in R2 bucket
     * @param contentType MIME type
     * @return Public accessible URL in R2
     */
    String uploadFromUrl(String sourceUrl, String targetKey, String contentType);

    /**
     * Delete an object from Cloudflare R2.
     *
     * @param key S3 object key
     */
    void deleteFile(String key);

    /**
     * Get the full public URL for a given object key.
     *
     * @param key S3 object key
     * @return Full URL
     */
    String getPublicUrl(String key);
}
