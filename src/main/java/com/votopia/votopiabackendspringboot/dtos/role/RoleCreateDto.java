package com.votopia.votopiabackendspringboot.dtos.role;

import com.votopia.votopiabackendspringboot.entities.auth.Permission;
import com.votopia.votopiabackendspringboot.entities.auth.Role;
import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.NonNull;

import java.util.Set;
import java.util.stream.Collectors;


public record RoleCreateDto (
    @NotBlank(message = "Il nome non può essere vuoto")
    @Size(max = 50)
    String name,

    @Nullable
    Long orgId,

    @Nullable
    Long listId,

    Set<Long> permissionsId,
    String color,
    @NonNull
    Integer level
) {
    public RoleCreateDto(Role r){
        this(
                r.getName(),
                r.getOrganization().getId(),
                r.getList().getId(),
                r.getPermissions().stream()
                        .map(Permission::getId)
                        .collect(Collectors.toSet()),
                r.getColor(),
                r.getLevel()
        );
    }
}
