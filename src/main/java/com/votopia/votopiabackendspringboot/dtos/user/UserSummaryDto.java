package com.votopia.votopiabackendspringboot.dtos.user;

import com.votopia.votopiabackendspringboot.dtos.organization.OrganizationSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.role.RoleSummaryDto;
import com.votopia.votopiabackendspringboot.entities.auth.User;
import lombok.*;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto {

    public UserSummaryDto(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.surname = user.getSurname();
        this.email = user.getEmail();

        this.org = new OrganizationSummaryDto(user.getOrg());

        if (user.getRoles() != null) {
            this.roles = user.getRoles().stream()
                    .map(RoleSummaryDto::new)
                    .collect(Collectors.toSet());
        }
        this.deleted = user.getDeleted();
        this.mustChangePassword = user.getMustChangePassword();
    }

    private Long id;
    private String name;
    private String surname;
    private String email;
    private OrganizationSummaryDto org;
    private Set<RoleSummaryDto> roles;
    private Boolean deleted;
    private Boolean mustChangePassword;
}