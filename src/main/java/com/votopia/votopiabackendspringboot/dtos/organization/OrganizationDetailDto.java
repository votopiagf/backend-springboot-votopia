package com.votopia.votopiabackendspringboot.dtos.organization;

import com.votopia.votopiabackendspringboot.entities.organizations.Organization;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

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
}
