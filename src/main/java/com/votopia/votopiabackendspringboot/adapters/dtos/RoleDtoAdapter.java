package com.votopia.votopiabackendspringboot.adapters.dtos;

import com.votopia.votopiabackendspringboot.dtos.role.RoleOptionDto;
import com.votopia.votopiabackendspringboot.entities.auth.Role;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adapter per convertire entità Role in DTO RoleOptionDto
 * Responsabile della trasformazione dati dal dominio alla presentazione
 */
@Component
public class RoleDtoAdapter {

    /**
     * Converte una singola entità Role in RoleOptionDto
     */
    public RoleOptionDto toOptionDto(Role role) {
        if (role == null) {
            return null;
        }
        return new RoleOptionDto(
                role.getId(),
                role.getName(),
                role.getColor(),
                role.getList() != null ? role.getList().getName() : null
        );
    }

    /**
     * Converte un set di Role in un set di RoleOptionDto
     */
    public Set<RoleOptionDto> toOptionDtos(Set<Role> roles) {
        if (roles == null) {
            return Set.of();
        }
        return roles.stream()
                .map(this::toOptionDto)
                .collect(Collectors.toSet());
    }
}

