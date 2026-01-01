package com.votopia.votopiabackendspringboot.dtos.user;

import com.votopia.votopiabackendspringboot.dtos.role.RoleSummaryDto;
import com.votopia.votopiabackendspringboot.entities.auth.User;
import lombok.*;

import java.util.Set;
import java.util.stream.Collectors;

public record UserSummaryDto(
        Long id,
        String name,
        String surname,
        String email,
        Set<RoleSummaryDto> roles
) {
    public UserSummaryDto(User u){
        this(
                u.getId(),
                u.getName(),
                u.getSurname(),
                u.getEmail(),
                u.getRoles().stream()
                        .map(RoleSummaryDto::new)
                        .collect(Collectors.toSet())
        );
    }
}