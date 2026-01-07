package com.votopia.votopiabackendspringboot.dtos.user;

import com.votopia.votopiabackendspringboot.dtos.list.ListOptionDto;
import com.votopia.votopiabackendspringboot.dtos.role.RoleOptionDto;

import java.util.Set;

/**
 * DTO contenente tutti i dati necessari per inizializzare la schermata di creazione utente nel frontend.
 * Viene restituito da un unico endpoint per evitare multiple richieste al server.
 */
public record UserCreationInitDto(
        Set<ListOptionDto> availableLists,
        Set<RoleOptionDto> availableRoles,
        Set<RoleOptionDto> availableRolesByList
) {
    /**
     * Costruttore factory method per facilitare la creazione.
     *
     * @param availableLists     Liste che l'utente può assegnare
     * @param availableRoles     Ruoli a livello organizzazione
     * @param availableRolesByList Ruoli disponibili per una lista (se richiesti)
     */
    public UserCreationInitDto(Set<ListOptionDto> availableLists, Set<RoleOptionDto> availableRoles, Set<RoleOptionDto> availableRolesByList) {
        this.availableLists = availableLists;
        this.availableRoles = availableRoles;
        this.availableRolesByList = availableRolesByList;
    }
}

