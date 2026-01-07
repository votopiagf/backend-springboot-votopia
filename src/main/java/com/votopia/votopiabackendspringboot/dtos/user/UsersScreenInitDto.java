package com.votopia.votopiabackendspringboot.dtos.user;

import com.votopia.votopiabackendspringboot.dtos.list.ListOptionDto;
import com.votopia.votopiabackendspringboot.dtos.role.RoleOptionDto;

import java.util.Set;

/**
 * DTO contenente TUTTE le informazioni necessarie per inizializzare la schermata Users nel frontend.
 * Include liste, ruoli, statistiche e dati filtrati in base ai permessi dell'utente.
 */
public record UsersScreenInitDto(
        Set<ListOptionDto> availableLists,
        Set<RoleOptionDto> availableOrgRoles,
        Set<RoleOptionDto> availableListRoles,
        Statistics statistics,
        FilterScope filterScope
) {
    /**
     * Statistiche aggregate visibili all'utente
     */
    public record Statistics(
            Long totalUsers,
            Long totalRoles,
            Long totalLists
    ) {}

    /**
     * Indica lo scope di filtro disponibile all'utente
     */
    public record FilterScope(
            boolean canFilterAllOrganization,
            boolean canFilterByList,
            Long restrictedToListId,
            String restrictedToListName
    ) {}

    public UsersScreenInitDto(
            Set<ListOptionDto> availableLists,
            Set<RoleOptionDto> availableOrgRoles,
            Set<RoleOptionDto> availableListRoles,
            Long totalUsers,
            Long totalRoles,
            Long totalLists,
            boolean canFilterAllOrganization,
            boolean canFilterByList,
            Long restrictedToListId,
            String restrictedToListName
    ) {
        this(
                availableLists,
                availableOrgRoles,
                availableListRoles,
                new Statistics(totalUsers, totalRoles, totalLists),
                new FilterScope(canFilterAllOrganization, canFilterByList, restrictedToListId, restrictedToListName)
        );
    }
}

