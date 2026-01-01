package com.votopia.votopiabackendspringboot.dtos.file;

import com.votopia.votopiabackendspringboot.dtos.list.ListSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.user.UserSummaryDto;
import com.votopia.votopiabackendspringboot.entities.files.File;
import com.votopia.votopiabackendspringboot.entities.files.FileCategory;

import java.time.LocalDateTime;

public record FileDetailDto(
        Long id,
        String name,
        ListSummaryDto list,
        UserSummaryDto user,
        FileCategory fileCategory,
        String filePath,
        String mimeType,
        LocalDateTime uploadedAt
) {
    public FileDetailDto(File f){
        this(
                f.getId(),
                f.getName(),
                new ListSummaryDto(f.getList()),
                new UserSummaryDto(f.getUser()),
                f.getFileCategory(),
                f.getFilePath(),
                f.getMimeType(),
                f.getUploadedAt()
        );
    }
}
