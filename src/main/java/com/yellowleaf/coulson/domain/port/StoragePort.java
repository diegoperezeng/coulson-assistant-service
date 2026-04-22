package com.yellowleaf.coulson.domain.port;

public interface StoragePort {

    String upload(String bucket, String objectPath, byte[] content, String contentType);

    void delete(String bucket, String objectPath);
}
