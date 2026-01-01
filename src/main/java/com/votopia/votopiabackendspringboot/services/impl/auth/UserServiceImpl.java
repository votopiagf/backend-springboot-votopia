package com.votopia.votopiabackendspringboot.services.impl.auth;

import com.votopia.votopiabackendspringboot.dtos.user.UserCreateDto;
import com.votopia.votopiabackendspringboot.dtos.user.UserSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.user.UserUpdateDto;
import com.votopia.votopiabackendspringboot.entities.lists.List;
import com.votopia.votopiabackendspringboot.entities.auth.Role;
import com.votopia.votopiabackendspringboot.entities.auth.User;
import com.votopia.votopiabackendspringboot.exceptions.ConflictException;
import com.votopia.votopiabackendspringboot.exceptions.NotFoundException;
import com.votopia.votopiabackendspringboot.repositories.lists.ListRepository;
import com.votopia.votopiabackendspringboot.repositories.auth.RoleRepository;
import com.votopia.votopiabackendspringboot.repositories.auth.UserRepository;
import com.votopia.votopiabackendspringboot.services.auth.AuthService;
import com.votopia.votopiabackendspringboot.services.auth.UserService;
import com.votopia.votopiabackendspringboot.services.auth.PermissionService;
import com.votopia.votopiabackendspringboot.exceptions.ForbiddenException;
import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementazione del servizio UserService.
 * Gestisce la registrazione, la visualizzazione, l'aggiornamento e la cancellazione (soft-delete) degli utenti.
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private ListRepository listRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthService authService;
    @Autowired private PermissionService permissionService;

    @Override
    @Transactional
    public UserSummaryDto register(UserCreateDto dto, Long authUserId) {
        User authUser = authService.getAuthenticatedUser(authUserId);

        // 1. Controllo Permessi
        boolean canOrg = permissionService.hasPermission(authUserId, "create_user_for_organization");
        boolean canList = permissionService.hasPermission(authUserId, "create_user_for_list");

        if (!canOrg && !canList) {
            throw new ForbiddenException("Non hai i permessi per creare utenti");
        }

        if (userRepository.existsByEmailAndOrgAndDeletedFalse(dto.email(), authUser.getOrg())) {
            throw new ConflictException("Utente con questa email già esiste nell'organizzazione");
        }

        // 2. Creazione Utente Base
        User newUser = new User();
        newUser.setName(dto.name());
        newUser.setSurname(dto.surname());
        newUser.setEmail(dto.email());
        newUser.setPassword(passwordEncoder.encode(dto.password()));
        newUser.setOrg(authUser.getOrg());

        // Salviamo per avere l'ID
        newUser = userRepository.save(newUser);

        // 3. Gestione Liste
        if (canOrg) {
            processOrgLists(dto.listsId(), newUser, authUser.getOrg().getId());
        } else {
            processRestrictedList(dto.listsId(), newUser, authUserId);
        }

        // 4. Gestione Ruoli
        processRoles(dto.rolesId(), newUser, authUserId);

        log.info("Utente registrato con successo: {} (ID: {})", newUser.getEmail(), newUser.getId());
        return new UserSummaryDto(userRepository.save(newUser));
    }

    @Override
    @Transactional
    public UserSummaryDto getUserInformation(Long authUserId, @Nullable Long targetUserId) {
        User authUser = authService.getAuthenticatedUser(authUserId);

        // Profilo personale
        if (targetUserId == null || targetUserId.equals(authUserId)) {
            return new UserSummaryDto(authUser);
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("Utente target non trovato"));

        // Multi-tenancy check
        if (!targetUser.getOrg().getId().equals(authUser.getOrg().getId())) {
            throw new ForbiddenException("Non puoi vedere utenti di altre organizzazioni");
        }

        // Controllo Permessi
        boolean permOrg = permissionService.hasPermission(authUserId, "view_all_user_organization");
        if (permOrg) return new UserSummaryDto(targetUser);

        boolean permLists = targetUser.getLists().stream()
                .anyMatch(list -> permissionService.hasPermissionOnList(authUserId, list.getId(), "view_all_user_list"));

        if (!permLists) {
            throw new ForbiddenException("Permesso negato per visualizzare questo utente");
        }

        return new UserSummaryDto(targetUser);
    }

    @Override
    @Transactional
    public Set<UserSummaryDto> getAllVisibleUsers(Long authUserId, @Nullable Long listId) {
        User authUser = authService.getAuthenticatedUser(authUserId);
        Long orgId = authUser.getOrg().getId();

        if (listId == null) {
            if (!permissionService.hasPermission(authUserId, "view_all_user_organization")) {
                throw new ForbiddenException("Permesso richiesto per vedere tutti gli utenti dell'organizzazione");
            }
            return userRepository.findAllByOrgId(orgId).stream()
                    .map(UserSummaryDto::new).collect(Collectors.toSet());
        }

        if (!permissionService.hasPermissionOnList(authUserId, listId, "view_all_user_list")) {
            throw new ForbiddenException("Non hai accesso agli utenti di questa lista");
        }

        return userRepository.findAllByListsId(listId).stream()
                .map(UserSummaryDto::new).collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public void delete(Long authUserId, Long targetUserId) {
        User authUser = authService.getAuthenticatedUser(authUserId);

        if (!permissionService.hasPermission(authUserId, "delete_user_organization")) {
            throw new ForbiddenException("Permesso di eliminazione richiesto");
        }

        User userToDelete = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("Utente da eliminare non trovato"));

        if (!userToDelete.getOrg().getId().equals(authUser.getOrg().getId())) {
            throw new ForbiddenException("Non puoi eliminare utenti di altre organizzazioni");
        }

        userToDelete.setDeleted(true);
        userRepository.save(userToDelete);
        log.info("Soft-delete eseguito per utente ID: {}", targetUserId);
    }

    @Override
    @Transactional
    public UserSummaryDto update(Long authUserId, UserUpdateDto dto) {
        Long targetId = (dto.id() != null) ? dto.id() : authUserId;
        User authUser = authService.getAuthenticatedUser(authUserId);
        User targetUser = userRepository.findById(targetId)
                .orElseThrow(() -> new NotFoundException("Target user not found"));

        // Validazione Permessi
        boolean isSelf = authUserId.equals(targetId);
        boolean hasOrg = permissionService.hasPermission(authUserId, "update_user_organization");
        boolean hasList = permissionService.hasPermission(authUserId, "update_user_list");

        validateUpdateAccess(authUser, targetUser, isSelf, hasOrg, hasList);

        // Update Campi
        if (dto.name() != null) targetUser.setName(dto.name());
        if (dto.surname() != null) targetUser.setSurname(dto.surname());
        if (dto.email() != null) targetUser.setEmail(dto.email());

        if (Boolean.TRUE.equals(dto.resetPassword())) {
            targetUser.setPassword(passwordEncoder.encode("Cambiami"));
            targetUser.setMustChangePassword(true);
        }

        // Gestione Liste (solo admin)
        if (hasOrg || hasList) {
            handleListUpdate(targetUser, authUserId, dto, hasOrg);
        }

        return new UserSummaryDto(userRepository.save(targetUser));
    }

    // --- HELPERS ---

    private void validateUpdateAccess(User auth, User target, boolean isSelf, boolean hasOrg, boolean hasList) {
        if (!auth.getOrg().getId().equals(target.getOrg().getId())) throw new ForbiddenException("Cross-org update negato");

        if (hasOrg) return;
        if (hasList && (isSelf || permissionService.checkSharedLists(auth.getId(), target.getId(), "update_user_list"))) return;
        if (isSelf) return;

        throw new ForbiddenException("Permessi insufficienti per modificare l'utente");
    }

    private void processOrgLists(Set<Long> listIds, User newUser, Long orgId) {
        if (listIds == null) return;
        for (Long id : listIds) {
            listRepository.findByIdAndOrgId(id, orgId).ifPresent(l -> {
                l.getUsers().add(newUser);
                listRepository.save(l);
            });
        }
    }

    private void processRestrictedList(Set<Long> listIds, User newUser, Long authUserId) {
        if (listIds == null || listIds.size() != 1) {
            throw new ForbiddenException("Puoi creare utenti in una sola lista alla volta");
        }
        Long targetId = listIds.iterator().next();
        if (!permissionService.hasPermissionOnList(authUserId, targetId, "create_user_for_list")) {
            throw new ForbiddenException("Non hai permessi su questa lista");
        }
        listRepository.findById(targetId).ifPresent(l -> l.getUsers().add(newUser));
    }

    private void processRoles(Set<Long> roleIds, User newUser, Long authUserId) {
        if (roleIds == null) return;
        for (Long roleId : roleIds) {
            Role role = roleRepository.findById(roleId).orElse(null);
            if (role == null) continue;

            int maxLevel = (role.getList() == null) ?
                    roleRepository.findMaxLevelByUserIdAndOrgId(authUserId, newUser.getOrg().getId()) :
                    roleRepository.findMaxLevelByUserIdAndListId(authUserId, role.getList().getId());

            if (role.getLevel() < maxLevel) {
                newUser.getRoles().add(role);
            }
        }
    }

    private void handleListUpdate(User target, Long authUserId, UserUpdateDto dto, boolean isOrgAdmin) {
        if (dto.removeLists() != null) {
            for (Long id : dto.removeLists()) {
                if (isOrgAdmin || permissionService.hasPermissionOnList(authUserId, id, "update_user_list")) {
                    target.getLists().removeIf(l -> l.getId().equals(id));
                    target.getRoles().removeIf(r -> r.getList() != null && r.getList().getId().equals(id));
                }
            }
        }
        if (dto.addLists() != null) {
            for (Long id : dto.addLists()) {
                if (isOrgAdmin || permissionService.hasPermissionOnList(authUserId, id, "update_user_list")) {
                    listRepository.findById(id).ifPresent(l -> {
                        if (l.getOrg().getId().equals(target.getOrg().getId())) target.getLists().add(l);
                    });
                }
            }
        }
    }
}