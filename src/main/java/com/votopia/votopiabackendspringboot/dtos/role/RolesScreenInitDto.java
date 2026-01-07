package com.votopia.votopiabackendspringboot.dtos.role;

import com.votopia.votopiabackendspringboot.dtos.permission.PermissionDetailDto;
import com.votopia.votopiabackendspringboot.dtos.permission.PermissionSummaryDto;

import java.util.List;
import java.util.Set;

public record RolesScreenInitDto(
        Set<RoleOptionDto> orgRoles,
        Set<RoleOptionDto> listRoles,
        Statistics statistics,
        FilterScope filterScope,
        List<PermissionDetailDto> userPermissions
) {
    public record Statistics(
            long totalRoles,
            long totalOrgRoles,
            long totalListRoles
    ) {}

    public record FilterScope(
            boolean canFilterAllOrganization,
            boolean canFilterByList,
            Long restrictedToListId,
            String restrictedToListName
    ) {}

    public RolesScreenInitDto(
            Set<RoleOptionDto> orgRoles,
            Set<RoleOptionDto> listRoles,
            long totalRoles,
            long totalOrgRoles,
            long totalListRoles,
            boolean canFilterAllOrganization,
            boolean canFilterByList,
            Long restrictedToListId,
            String restrictedToListName,
            List<PermissionDetailDto> userPermissions
    ) {
        this(
                orgRoles,
                listRoles,
                new Statistics(totalRoles, totalOrgRoles, totalListRoles),
                new FilterScope(canFilterAllOrganization, canFilterByList, restrictedToListId, restrictedToListName),
                userPermissions
        );
    }
}
