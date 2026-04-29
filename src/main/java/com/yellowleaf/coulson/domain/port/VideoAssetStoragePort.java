package com.yellowleaf.coulson.domain.port;

import java.nio.file.Path;

public interface VideoAssetStoragePort {

    String upload(String bucket, String key, Path filePath, String contentType);

    String getPublicUrl(String bucket, String key);

    String getSignedUrl(String bucket, String key, long expiresInSeconds);
}
