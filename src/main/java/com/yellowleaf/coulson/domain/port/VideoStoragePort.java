package com.yellowleaf.coulson.domain.port;

import com.yellowleaf.coulson.domain.model.Video;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface VideoStoragePort {

    Video store(MultipartFile file);

    void delete(Path filePath);
}
