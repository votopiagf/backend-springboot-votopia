package com.votopia.votopiabackendspringboot.dtos.list;

import com.votopia.votopiabackendspringboot.entities.lists.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

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
}