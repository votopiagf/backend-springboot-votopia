package com.votopia.votopiabackendspringboot.dtos.organization;

import com.votopia.votopiabackendspringboot.entities.organizations.Organization;
import jakarta.validation.constraints.NotNull;

public record OrganizationUpdateDto(
    @NotNull(message = "L'id non può essere vuoto")
    Long id,

    String name,
    Organization.Status status,
    Boolean resetCode,
    Long planId
) {
}
