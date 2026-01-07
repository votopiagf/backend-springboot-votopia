package com.votopia.votopiabackendspringboot.dtos.role;

import com.votopia.votopiabackendspringboot.entities.auth.Role;

/**
 * DTO minimalista per dropdown/checkbox di ruoli durante creazione utente.
 * Mostra solo ID, nome, colore e nome della lista (se disponibile).
 */
public record RoleOptionDto(
        Long id,
        String name,
        String color,
        String listName
) {
    public RoleOptionDto(Role role) {
        this(
                role.getId(),
                role.getName(),
                role.getColor(),
                role.getList() != null ? role.getList().getName() : null
        );
    }
}

