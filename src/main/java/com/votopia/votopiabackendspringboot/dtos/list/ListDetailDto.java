package com.votopia.votopiabackendspringboot.dtos.list;

import com.votopia.votopiabackendspringboot.dtos.file.FileSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.school.SchoolSummaryDto;
import com.votopia.votopiabackendspringboot.entities.lists.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.LocalDateTime;
/*
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListBasicDto {
    public ListBasicDto(List list) {
        this.id = list.getId();
        this.name = list.getName();
        this.description = list.getDescription();
    }

    @NonNull
    private Long id;

    @NonNull
    private String name;

    @NonNull
    private String description;
}*/

public record ListDetailDto(
        Long id,
        String name,
        String description,
        SchoolSummaryDto school,
        String slogan,
        String colorPrimary,
        String colorSecondary,
        FileSummaryDto file,
        LocalDateTime createdAt
){
    public ListDetailDto(List l){
        this(
                l.getId(),
                l.getName(),
                l.getDescription(),
                new SchoolSummaryDto(l.getSchool()),
                l.getSlogan(),
                l.getColorPrimary(),
                l.getColorSecondary(),
                new FileSummaryDto(l.getLogoFile()),
                l.getCreatedAt()
        );
    }
}