package com.yellowleaf.coulson.application.command;

import org.springframework.web.multipart.MultipartFile;

public class ProcessVideoCommand {

    private final MultipartFile file;

    public ProcessVideoCommand(MultipartFile file) {
        this.file = file;
    }

    public MultipartFile getFile() {
        return file;
    }
}
