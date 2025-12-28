package com.votopia.votopiabackendspringboot.services.auth;

import com.votopia.votopiabackendspringboot.dtos.permission.PermissionSummaryDto;
import com.votopia.votopiabackendspringboot.entities.auth.User;
import com.votopia.votopiabackendspringboot.entities.lists.List; // La tua entità

import java.util.Set;


public interface PermissionService {

    /**
     * Controlla se un utente ha un determinato permesso tramite i suoi ruoli.
     * Corrisponde a 'check_user_permission' in Django.
     */
    boolean hasPermission(Long userId, String permissionName);

    /**
     * Recupera la lista di tutti i permessi associati all'utente.
     * Corrisponde a 'get_user_permissions' in Django.
     */
    java.util.List<PermissionSummaryDto> getUserPermissions(Long userId);

    /**
     * Verifica l'esistenza di un utente attivo (non eliminato logicamente).
     * Corrisponde a 'verify_user_exists' in Django.
     */
    boolean verifyUserExists(Long userId);

    /**
     * Restituisce le liste su cui l'utente ha potere, in base a un permesso specifico.
     * Se ha il permesso globale, vede tutte le liste dell'organizzazione.
     * Altrimenti solo quelle di cui è membro.
     */
    Set<List> getListsUserHasPermission(User user, String permissionName);

    boolean hasPermissionOnList(Long userId, Long listId, String permissionName);

    boolean checkSharedLists(Long authUserId, Long targetUserId, String permissionCode);

    void validatePermission(Long authUserId, Long listId, String orgPerm, String listPerm, String errorMsg);
}