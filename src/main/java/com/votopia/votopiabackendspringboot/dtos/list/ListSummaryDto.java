package com.votopia.votopiabackendspringboot.dtos.list;

import com.votopia.votopiabackendspringboot.dtos.file.FileSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.school.SchoolSummaryDto;
import com.votopia.votopiabackendspringboot.entities.lists.List;

/**
 * DTO per la visualizzazione sintetica di una Lista.
 */
public record ListSummaryDto(
        Long id,
        String name,
        SchoolSummaryDto school,
        String slogan,
        FileSummaryDto logoFile
) {
    /**
     * Costruttore per convertire l'entità List nel DTO.
     * * @param list L'entità List da mappare.
     */
    public ListSummaryDto(List list) {
        this(
                list.getId(),
                list.getName(),
                list.getSchool() != null ? new SchoolSummaryDto(list.getSchool()) : null,
                list.getSlogan(),
                list.getLogoFile() != null ? new FileSummaryDto(list.getLogoFile()) : null
        );
    }
}