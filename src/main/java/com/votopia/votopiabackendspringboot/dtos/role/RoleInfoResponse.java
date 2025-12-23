package com.votopia.votopiabackendspringboot.dtos.role;

import java.util.Set;

public record RoleInfoResponse(
        Set<RoleSummaryDto> roles,
        Set<RoleSummaryDto> userRoles, // Popolato solo in caso di 403 o diagnostica
        Integer count
) {}