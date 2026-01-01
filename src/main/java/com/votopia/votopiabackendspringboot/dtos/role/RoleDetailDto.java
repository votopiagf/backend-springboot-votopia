package com.votopia.votopiabackendspringboot.dtos.role;

import com.votopia.votopiabackendspringboot.dtos.list.ListSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.permission.PermissionSummaryDto;
import com.votopia.votopiabackendspringboot.entities.auth.Permission;
import com.votopia.votopiabackendspringboot.entities.auth.Role;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record RoleDetailDto(
        Long id,
        ListSummaryDto list,
        String name,
        String color,
        Integer level,
        Set<PermissionSummaryDto> permissions,
        LocalDateTime createdAt
) {
    public RoleDetailDto(Role r){
        this(
                r.getId(),
                new ListSummaryDto(r.getList()),
                r.getName(),
                r.getColor(),
                r.getLevel(),
                r.getPermissions().stream()
                        .map(PermissionSummaryDto::new)
                        .collect(Collectors.toSet()),
                r.getCreatedAt()
        );
    }
}
