package com.votopia.votopiabackendspringboot.dtos.permission;

import com.votopia.votopiabackendspringboot.entities.auth.Permission;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PermissionSummaryDto {
    public PermissionSummaryDto(Permission p){
        this.id = p.getId();
        this.name = p.getName();
        this.description = p.getDescription();
    }
    @NonNull
    private Long id;
    @NonNull
    private String name;
    private String description;
}