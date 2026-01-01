package com.votopia.votopiabackendspringboot.services.impl.auth;

import com.votopia.votopiabackendspringboot.dtos.role.RoleInfoResponse;
import com.votopia.votopiabackendspringboot.dtos.permission.PermissionSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.role.RoleCreateDto;
import com.votopia.votopiabackendspringboot.dtos.role.RoleSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.role.RoleUpdateDto;
import com.votopia.votopiabackendspringboot.entities.lists.List;
import com.votopia.votopiabackendspringboot.entities.auth.Permission;
import com.votopia.votopiabackendspringboot.entities.auth.Role;
import com.votopia.votopiabackendspringboot.entities.auth.User;
import com.votopia.votopiabackendspringboot.exceptions.ForbiddenException;
import com.votopia.votopiabackendspringboot.exceptions.NotFoundException;
import com.votopia.votopiabackendspringboot.repositories.lists.ListRepository;
import com.votopia.votopiabackendspringboot.repositories.auth.PermissionRepository;
import com.votopia.votopiabackendspringboot.repositories.auth.RoleRepository;
import com.votopia.votopiabackendspringboot.services.auth.AuthService;
import com.votopia.votopiabackendspringboot.services.auth.PermissionService;
import com.votopia.votopiabackendspringboot.services.auth.RoleService;
import io.micrometer.common.lang.Nullable;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RoleServiceImpl implements RoleService {

    @Autowired private RoleRepository roleRepository;
    @Autowired private AuthService authService;
    @Autowired private PermissionService permissionService;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private ListRepository listRepository;

    @Override
    @Transactional
    public RoleSummaryDto create(RoleCreateDto dto, Long authUserId) {
        User authUser = authService.getAuthenticatedUser(authUserId);
        boolean isListRole = dto.listId() != null;

        // 1. Controllo Possessione Permessi (L'utente può dare solo ciò che ha)
        validatePermissionsPossession(authUserId, dto.permissionsId());

        // 2. Preparazione Ruolo e Calcolo Livello Massimo
        Role newRole = new Role();
        newRole.setName(dto.name());
        newRole.setColor(dto.color());
        newRole.setOrganization(authUser.getOrg());

        int maxAuthLevel;
        if (isListRole) {
            List targetList = validateListAndGet(authUser, dto.listId(), "create_role_list", "create_role_organization");
            newRole.setList(targetList);
            maxAuthLevel = roleRepository.findMaxLevelByUserIdAndListId(authUserId, dto.listId());
        } else {
            validateGlobalPermission(authUserId, "create_role_organization", "Permesso 'create_role_organization' richiesto.");
            maxAuthLevel = roleRepository.findMaxLevelByUserIdAndOrgId(authUserId, authUser.getOrg().getId());
        }

        // 3. Controllo Gerarchico
        if (dto.level() >= maxAuthLevel) {
            throw new ForbiddenException("Violazione gerarchica: non puoi creare un ruolo di livello " + dto.level() + " (Max: " + maxAuthLevel + ")");
        }
        newRole.setLevel(dto.level());

        // 4. Assegnazione Permessi
        assignPermissionsToRole(newRole, dto.permissionsId());

        Role savedRole = roleRepository.save(newRole);
        log.info("Ruolo creato: {} (ID: {}) contesto: {}", savedRole.getName(), savedRole.getId(), isListRole ? "LIST" : "ORG");
        return new RoleSummaryDto(savedRole);
    }

    @Override
    @Transactional
    public void delete(Long roleId, Long authUserId) {
        User authUser = authService.getAuthenticatedUser(authUserId);
        Role roleTarget = getRoleAndValidateOrg(roleId, authUser);

        // Calcolo autorità nel contesto del ruolo target
        int maxAuthLevel;
        if (roleTarget.getList() == null) {
            validateGlobalPermission(authUserId, "delete_role_organization", "Permesso delete_role_organization richiesto.");
            maxAuthLevel = roleRepository.findMaxLevelByUserIdAndOrgId(authUserId, authUser.getOrg().getId());
        } else {
            Long listId = roleTarget.getList().getId();
            validatePermissionInContext(authUserId, listId, "delete_role_organization", "delete_role_list", "Permessi insufficienti per eliminare ruoli in questa lista.");
            maxAuthLevel = roleRepository.findMaxLevelByUserIdAndListId(authUserId, listId);
        }

        if (roleTarget.getLevel() >= maxAuthLevel) {
            throw new ForbiddenException("Violazione gerarchica: non puoi eliminare un ruolo di livello superiore o uguale al tuo.");
        }

        roleRepository.delete(roleTarget);
    }

    @Override
    @Transactional
    public Set<RoleSummaryDto> getAllVisible(Long authUserId, @Nullable Long listId) {
        User authUser = authService.getAuthenticatedUser(authUserId);
        Long orgId = authUser.getOrg().getId();

        if (listId == null) {
            validateGlobalPermission(authUserId, "view_all_role_organization", "Richiesto permesso Org per vista globale.");
            return roleRepository.findAllByOrganizationId(orgId).stream().map(RoleSummaryDto::new).collect(Collectors.toSet());
        }

        List targetList = listRepository.findById(listId).orElseThrow(() -> new NotFoundException("Lista non trovata."));
        if (!targetList.getOrg().getId().equals(orgId)) throw new NotFoundException("Lista non trovata nell'organizzazione.");

        validatePermissionInContext(authUserId, listId, "view_all_role_organization", "view_all_role_list", "Accesso negato ai ruoli della lista.");

        return roleRepository.findAllByListId(listId).stream().map(RoleSummaryDto::new).collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public RoleInfoResponse getRoleInformation(Long authUserId, Long roleId) {
        User authUser = authService.getAuthenticatedUser(authUserId);
        Set<RoleSummaryDto> authUserRoles = authUser.getRoles().stream().map(RoleSummaryDto::new).collect(Collectors.toSet());

        if (roleId == null) return new RoleInfoResponse(authUserRoles, null, authUserRoles.size());

        Role targetRole = getRoleAndValidateOrg(roleId, authUser);

        boolean authorized = permissionService.hasPermission(authUserId, "view_all_role_organization") ||
                (targetRole.getList() != null && permissionService.hasPermissionOnList(authUserId, targetRole.getList().getId(), "view_all_role_list"));

        if (!authorized) throw new ForbiddenException("Permessi insufficienti per visualizzare il ruolo.");

        return new RoleInfoResponse(Set.of(new RoleSummaryDto(targetRole)), null, 1);
    }

    @Override
    @Transactional
    public RoleSummaryDto update(RoleUpdateDto dto, Long authUserId) {
        User authUser = authService.getAuthenticatedUser(authUserId);
        Role roleTarget = getRoleAndValidateOrg(dto.id(), authUser);

        int maxAuthLevel;
        if (roleTarget.getList() == null) {
            validateGlobalPermission(authUserId, "update_role_organization", "Permesso update_role_organization richiesto.");
            maxAuthLevel = roleRepository.findMaxLevelByUserIdAndOrgId(authUserId, authUser.getOrg().getId());
        } else {
            Long listId = roleTarget.getList().getId();
            validatePermissionInContext(authUserId, listId, "update_role_organization", "update_role_list", "Permessi insufficienti per modificare ruoli lista.");
            maxAuthLevel = roleRepository.findMaxLevelByUserIdAndListId(authUserId, listId);
        }

        if (roleTarget.getLevel() >= maxAuthLevel) throw new ForbiddenException("Impossibile modificare un ruolo di livello pari o superiore.");

        if (dto.name() != null) roleTarget.setName(dto.name());
        if (dto.color() != null) roleTarget.setColor(dto.color());
        if (dto.level() != null) {
            if (dto.level() > maxAuthLevel) throw new ForbiddenException("Livello troppo alto.");
            roleTarget.setLevel(dto.level());
        }

        if (dto.permissions() != null) {
            validatePermissionsPossession(authUserId, dto.permissions());
            assignPermissionsToRole(roleTarget, dto.permissions());
        }

        return new RoleSummaryDto(roleRepository.save(roleTarget));
    }

    // --- HELPERS PRIVATI ---

    private Role getRoleAndValidateOrg(Long roleId, User authUser) {
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new NotFoundException("Ruolo non trovato"));
        if (!role.getOrganization().getId().equals(authUser.getOrg().getId())) {
            throw new ForbiddenException("Il ruolo appartiene a un'altra organizzazione.");
        }
        return role;
    }

    private void validateGlobalPermission(Long authUserId, String permission, String errorMsg) {
        if (!permissionService.hasPermission(authUserId, permission)) {
            throw new ForbiddenException(errorMsg);
        }
    }

    private void validatePermissionInContext(Long authUserId, Long listId, String orgPerm, String listPerm, String error) {
        boolean hasAccess = permissionService.hasPermission(authUserId, orgPerm) ||
                permissionService.hasPermissionOnList(authUserId, listId, listPerm);
        if (!hasAccess) throw new ForbiddenException(error);
    }

    private List validateListAndGet(User authUser, Long listId, String listPerm, String orgPerm) {
        List list = listRepository.findById(listId).orElseThrow(() -> new NotFoundException("Lista non trovata."));
        if (!list.getOrg().getId().equals(authUser.getOrg().getId())) throw new ForbiddenException("Lista non valida.");

        validatePermissionInContext(authUser.getId(), listId, orgPerm, listPerm, "Permessi insufficienti per agire sulla lista.");
        return list;
    }

    private void validatePermissionsPossession(Long authUserId, Set<Long> permsToAssign) {
        if (permsToAssign == null || permsToAssign.isEmpty()) return;
        Set<Long> userPermIds = permissionService.getUserPermissions(authUserId).stream()
                .map(PermissionSummaryDto::id).collect(Collectors.toSet());
        if (!userPermIds.containsAll(permsToAssign)) {
            throw new ForbiddenException("Non puoi assegnare permessi che non possiedi.");
        }
    }

    private void assignPermissionsToRole(Role role, Set<Long> permissionIds) {
        if (permissionIds == null) return;
        java.util.List<Permission> perms = permissionRepository.findAllById(permissionIds);
        if (perms.size() != permissionIds.size()) throw new NotFoundException("Uno o più permessi non esistono.");
        role.setPermissions(new HashSet<>(perms));
    }
}