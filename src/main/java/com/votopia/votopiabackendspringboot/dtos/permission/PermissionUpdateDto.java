package com.votopia.votopiabackendspringboot.dtos.permission;

import com.votopia.votopiabackendspringboot.entities.auth.Permission;
import jakarta.validation.constraints.NotNull;

public record PermissionUpdateDto(
        @NotNull(message = "L'id non può essere vuoto")
        Long id,

        String name,
        String description
) {
    public PermissionUpdateDto(Permission p){
        this(
                p.getId(),
                p.getName(),
                p.getDescription()
        );
    }
}
