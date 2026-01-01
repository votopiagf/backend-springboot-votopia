package com.votopia.votopiabackendspringboot.dtos.organization;

import com.votopia.votopiabackendspringboot.dtos.plan.PlanDetailDto;
import com.votopia.votopiabackendspringboot.dtos.plan.PlanSummaryDto;
import com.votopia.votopiabackendspringboot.entities.organizations.Organization;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/*
@Data
@Getter
@Setter
public class OrganizationBasicDto {
    private Long id;
    private String code;
    private String name;

    public OrganizationBasicDto(Organization o){
        this.id = o.getId();
        this.code = o.getCode();
        this.name = o.getName();
    }
}*/

public record OrganizationDetailDto(
        Long id,
        String code,
        String name,
        PlanSummaryDto plan,
        Organization.Status status,
        Integer maxLists,
        LocalDateTime createdAt
) {
    public OrganizationDetailDto(Organization o){
        this(
                o.getId(),
                o.getCode(),
                o.getName(),
                new PlanSummaryDto(o.getPlan()),
                o.getStatus(),
                o.getMaxLists(),
                o.getCreatedAt()
        );
    }
}
