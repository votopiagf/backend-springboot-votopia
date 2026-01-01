package com.votopia.votopiabackendspringboot.dtos.organization;

import com.votopia.votopiabackendspringboot.entities.organizations.Organization;
import com.votopia.votopiabackendspringboot.entities.organizations.Plan;

public record OrganizationSummaryDto(
        Long id,
        String code,
        String name,
        Organization.Status status
)
{
    public OrganizationSummaryDto(Organization o){
        this(
                o.getId(),
                o.getCode(),
                o.getName(),
                o.getStatus()
        );
    }
}
