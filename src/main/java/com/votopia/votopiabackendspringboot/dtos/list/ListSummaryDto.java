package com.votopia.votopiabackendspringboot.dtos.list;

import com.votopia.votopiabackendspringboot.entities.lists.List;

/**
 * DTO per la visualizzazione sintetica di una Lista.
 */
public record ListSummaryDto(
        Long id,
        String name,
        Long schoolId,
        String description,
        String slogan,
        String colorPrimary,
        String colorSecondary,
        Long logoFileId
) {
    /**
     * Costruttore per convertire l'entità List nel DTO.
     * * @param list L'entità List da mappare.
     */
    public ListSummaryDto(List list) {
        this(
                list.getId(),
                list.getName(),
                list.getSchool() != null ? list.getSchool().getId() : null,
                list.getDescription(),
                list.getSlogan(),
                list.getColorPrimary(),
                list.getColorSecondary(),
                list.getLogoFile() != null ? list.getLogoFile().getId() : null
        );
    }
}