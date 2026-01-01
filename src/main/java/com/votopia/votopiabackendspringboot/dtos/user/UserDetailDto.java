package com.votopia.votopiabackendspringboot.dtos.user;

import com.votopia.votopiabackendspringboot.dtos.list.ListSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.role.RoleDetailDto;
import com.votopia.votopiabackendspringboot.dtos.role.RoleSummaryDto;
import com.votopia.votopiabackendspringboot.entities.auth.User;

import java.util.Set;
import java.util.stream.Collectors;

public record UserDetailDto(
    Long id,
    String name,
    String surname,
    String email,
    Set<RoleSummaryDto> roles,
    Boolean deleted,
    Boolean mustChangePassword,
    Set<ListSummaryDto> list
) {
    public UserDetailDto(User u){
        this(
                u.getId(),
                u.getName(),
                u.getSurname(),
                u.getEmail(),
                u.getRoles().stream()
                        .map(RoleSummaryDto::new)
                        .collect(Collectors.toSet()),
                u.getDeleted(),
                u.getMustChangePassword(),
                u.getLists().stream()
                        .map(ListSummaryDto::new)
                        .collect(Collectors.toSet())
        );
    }
}
