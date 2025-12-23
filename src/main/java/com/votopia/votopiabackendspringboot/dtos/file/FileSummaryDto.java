package com.votopia.votopiabackendspringboot.dtos.file;

import com.votopia.votopiabackendspringboot.entities.File;
import java.time.LocalDateTime;

/**
 * DTO per la rappresentazione sintetica dei metadati di un file caricato.
 */
public record FileSummaryDto(
        Long id,
        String name,
        String filePath,
        String mimeType,
        Long listId,
        Long categoryId,
        Long userId,
        LocalDateTime uploadedAt
) {
    /**
     * Costruttore compatto per mappare l'entità File nel DTO.
     * * @param file L'entità JPA File da convertire.
     */
    public FileSummaryDto(File file) {
        this(
                file.getId(),
                file.getName(),
                file.getFilePath(),
                file.getMimeType(),
                file.getList() != null ? file.getList().getId() : null,
                file.getFileCategory() != null ? file.getFileCategory().getId() : null,
                file.getUser() != null ? file.getUser().getId() : null,
                file.getUploadedAt()
        );
    }
}