package com.votopia.votopiabackendspringboot.dtos.file;

import com.votopia.votopiabackendspringboot.entities.files.File;

public record FileBasicDto(
        Long id,
        String name,
        String filePath,
        String mimeType
) {
    public FileBasicDto(File f){
        this(
                f.getId(),
                f.getName(),
                f.getFilePath(),
                f.getMimeType()
        );
    }
}
