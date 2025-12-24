package com.votopia.votopiabackendspringboot.dtos.role;

import com.votopia.votopiabackendspringboot.dtos.list.ListBasicDto;
import com.votopia.votopiabackendspringboot.dtos.permission.PermissionSummaryDto;
import com.votopia.votopiabackendspringboot.entities.auth.Role;
import lombok.*;

import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Getter
public class RoleSummaryDto {
    public RoleSummaryDto(Role role){
        this.id = role.getId();
        this.name = role.getName();
        this.list = (role.getList() != null) ? new ListBasicDto(role.getList()):null;
        if (role.getPermissions() != null) {
            this.permissions = role.getPermissions().stream()
                    .map(PermissionSummaryDto::new)
                    .collect(Collectors.toSet());
        }
    }

    @NonNull
    private Long id;

    private ListBasicDto list;

    @NonNull
    private String name;

    private Set<PermissionSummaryDto> permissions;
}
