package com.votopia.votopiabackendspringboot.dtos.list;

import com.votopia.votopiabackendspringboot.entities.lists.List;

/**
 * DTO minimalista per dropdown/checkbox di liste durante creazione utente.
 * Mostra solo ID, nome e scuola (organizzazione).
 */
public record ListOptionDto(
        Long id,
        String name,
        String school
) {
    public ListOptionDto(List list) {
        this(
                list.getId(),
                list.getName(),
                list.getOrg() != null ? list.getOrg().getName() : null
        );
    }
}

