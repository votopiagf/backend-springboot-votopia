package com.votopia.votopiabackendspringboot.dtos.organization;

import com.votopia.votopiabackendspringboot.entities.organizations.Organization;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class OrganizationSummaryDto {
    private Long id;
    private String code;
    private String name;

    public OrganizationSummaryDto(Organization o){
        this.id = o.getId();
        this.code = o.getCode();
        this.name = o.getName();
    }
}
