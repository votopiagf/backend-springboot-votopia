package com.votopia.votopiabackendspringboot.services.impl.auth;

import com.votopia.votopiabackendspringboot.dtos.permission.PermissionSummaryDto;
import com.votopia.votopiabackendspringboot.entities.lists.List;
import com.votopia.votopiabackendspringboot.entities.auth.User;
import com.votopia.votopiabackendspringboot.exceptions.NotFoundException;
import com.votopia.votopiabackendspringboot.repositories.lists.ListRepository;
import com.votopia.votopiabackendspringboot.repositories.auth.UserRepository;
import com.votopia.votopiabackendspringboot.services.auth.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PermissionServiceImpl implements PermissionService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ListRepository listRepository;

    @Override
    public boolean hasPermission(Long userId, String permissionName) {
        try {
            return userRepository.hasPermission(userId, permissionName);
        } catch (Exception e) {
            log.error("Errore nel controllo permessi per user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    @Override
    public java.util.List<PermissionSummaryDto> getUserPermissions(Long userId) {
        return userRepository.findAllPermissionsByUserId(userId).stream()
                .map(p -> new PermissionSummaryDto(p.getId(), p.getName(), p.getDescription())) // Niente cast qui!
                .collect(Collectors.toList());
    }

    @Override
    public boolean verifyUserExists(Long userId) {
        return userRepository.existsByIdAndDeletedFalse(userId);
    }

    @Override
    public Set<List> getListsUserHasPermission(User user, String permissionName) {
        // Controlla se l'utente ha il permesso globale tramite i suoi ruoli
        boolean hasGlobalPermission = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(p -> p.getName().equals(permissionName));

        if (hasGlobalPermission) {
            // L'utente può vedere tutte le liste della sua organizzazione
            return listRepository.findListsByOrg(user.getOrg());
        } else {
            // L'utente vede solo le liste in cui è effettivamente membro
            // (Assumendo che tu abbia una relazione ManyToMany tra User e List)
            return user.getLists();
        }
    }

    @Override
    public boolean hasPermissionOnList(Long userId, Long listId, String permissionName) {
        // Recuperiamo l'utente con i suoi ruoli
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;

        // Cerchiamo tra i ruoli dell'utente
        return user.getRoles().stream()
                .filter(role -> role.getList() != null && role.getList().getId().equals(listId)) // Solo ruoli di quella lista
                .flatMap(role -> role.getPermissions().stream()) // "Apriamo" i permessi di quei ruoli
                .anyMatch(p -> p.getName().equals(permissionName)); // Controlliamo se il nome coincide
    }

    /**
     * Verifica se l'amministratore condivide una lista con il target
     * ed è autorizzato a gestirla.
     */
    @Override
    public boolean checkSharedLists(Long authUserId, Long targetUserId, String permissionCode) {
        // Recuperiamo le liste del target
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("Target non trovato"));

        // Controlliamo se per almeno una lista del target, l'admin ha il permesso richiesto
        return target.getLists().stream()
                .anyMatch(list -> hasPermissionOnList(authUserId, list.getId(), permissionCode));
    }
}