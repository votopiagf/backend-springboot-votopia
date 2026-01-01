package com.votopia.votopiabackendspringboot.dtos.permission;

import com.votopia.votopiabackendspringboot.dtos.role.RoleSummaryDto;
import com.votopia.votopiabackendspringboot.entities.auth.Permission;

import java.util.Set;
import java.util.stream.Collectors;

public record PermissionDetailDto(
        Long id,
        String name,
        String description,
        Set<RoleSummaryDto> roles
) {
    public PermissionDetailDto(Permission p){
        this(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getRoles().stream()
                        .map(RoleSummaryDto::new)
                        .collect(Collectors.toSet())
        );
    }
}
