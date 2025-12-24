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
import com.votopia.votopiabackendspringboot.repositories.auth.UserRepository;
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

    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private PermissionRepository permissionRepository;
    @Autowired
    private ListRepository listRepository;

    @Override
    @Transactional
    public RoleSummaryDto create(RoleCreateDto dto, Long authUserId) {
        // 0. Recupero Utente Autenticato
        User authUser = userRepository.findById(authUserId)
                .orElseThrow(() -> new NotFoundException("Utente autenticato non trovato"));

        // 1. Validazione Contesto
        boolean isListRole = dto.getListId() != null;

        // 2. Controllo Possessione Permessi (L'utente può dare solo ciò che ha)
        // Estraiamo i Long dal PermissionSummaryDto restituito dal service
        Set<Long> userPermIds = permissionService.getUserPermissions(authUserId).stream()
                .map(PermissionSummaryDto::getId)
                .collect(Collectors.toSet());

        if (dto.getPermissionsId() != null && !userPermIds.containsAll(dto.getPermissionsId())) {
            throw new ForbiddenException("Non puoi assegnare permessi che non possiedi personalmente.");
        }

        // 3. Preparazione Nuovo Ruolo
        int maxAuthLevel;
        Role newRole = new Role();
        newRole.setName(dto.getName());
        newRole.setColor(dto.getColor());
        newRole.setOrganization(authUser.getOrg());

        if (isListRole) {
            List targetList = validateListContext(authUser, dto.getListId());
            newRole.setList(targetList);
            maxAuthLevel = roleRepository.findMaxLevelByUserIdAndListId(authUserId, dto.getListId());
        } else {
            maxAuthLevel = roleRepository.findMaxLevelByUserIdAndOrgId(authUserId, authUser.getOrg().getId());
        }

        // 4. Controllo Gerarchico
        if (dto.getLevel() >= maxAuthLevel) {
            throw new ForbiddenException("Violazione gerarchica: il tuo livello massimo è " + maxAuthLevel +
                    ". Non puoi creare un ruolo di livello " + dto.getLevel());
        }
        newRole.setLevel(dto.getLevel());

        // 5. Assegnazione Permessi Fisici (usando List<Long> dal DTO)
        if (dto.getPermissionsId() != null && !dto.getPermissionsId().isEmpty()) {
            // Cerchiamo le entità Permission tramite gli ID presenti nel DTO
            java.util.List<Permission> foundPerms = permissionRepository.findAllById(dto.getPermissionsId());

            if (foundPerms.size() != dto.getPermissionsId().size()) {
                throw new NotFoundException("Uno o più permessi specificati non esistono nel database.");
            }

            newRole.setPermissions(new HashSet<>(foundPerms));
        }

        // 6. Salvataggio
        Role savedRole = roleRepository.save(newRole);
        log.info("Ruolo creato: {} (ID: {}) contesto: {}", savedRole.getName(), savedRole.getId(), isListRole ? "LIST" : "ORG");

        return new RoleSummaryDto(savedRole);
    }

    private List validateListContext(User authUser, Long listId) {
        List list = listRepository.findById(listId)
                .orElseThrow(() -> new NotFoundException("Lista target non trovata."));

        if (!list.getOrg().getId().equals(authUser.getOrg().getId())) {
            throw new ForbiddenException("La lista non appartiene alla tua organizzazione.");
        }

        boolean hasPerm = permissionService.hasPermissionOnList(authUser.getId(), listId, "create_role_list") ||
                permissionService.hasPermission(authUser.getId(), "create_role_organization");

        if (!hasPerm) {
            throw new ForbiddenException("Non hai i permessi per creare ruoli in questa lista.");
        }

        return list;
    }

    @Override
    @Transactional
    public void delete(Long roleId, Long authUserId) {
        // 1. Recupero Utente e Ruolo Target
        User user = userRepository.findById(authUserId)
                .orElseThrow(() -> new NotFoundException("Utente autenticato non trovato"));

        Role roleTarget = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Ruolo con ID " + roleId + " non trovato"));

        // 2. Controllo Organizzazione
        if (!roleTarget.getOrganization().getId().equals(user.getOrg().getId())) {
            log.warn("Tentativo di cancellazione cross-org: User {} su Role {}", authUserId, roleId);
            throw new ForbiddenException("Non puoi eliminare ruoli appartenenti ad altre organizzazioni.");
        }

        // 3. Controllo Permessi Generali
        boolean canOrg = permissionService.hasPermission(authUserId, "delete_role_organization");
        boolean canList = permissionService.hasPermission(authUserId, "delete_role_list");

        if (!canOrg && !canList) {
            throw new ForbiddenException("Non hai i permessi necessari per eliminare ruoli.");
        }

        // 4. Verifica Contesto e Calcolo Max Level (Gerarchia)
        boolean isOrgRoleTarget = roleTarget.getList() == null;
        int maxAuthLevel = 0;

        if (isOrgRoleTarget) {
            // Se il ruolo è Org, serve obbligatoriamente il permesso Org
            if (!canOrg) {
                throw new ForbiddenException("Permesso delete_role_organization richiesto per eliminare ruoli Org.");
            }
            maxAuthLevel = roleRepository.findMaxLevelByUserIdAndOrgId(authUserId, user.getOrg().getId());
        } else {
            // Se il ruolo è di Lista, serve il permesso Org OPPURE il permesso specifico sulla lista
            Long listId = roleTarget.getList().getId();
            if (!canOrg) {
                boolean hasListPerm = permissionService.hasPermissionOnList(authUserId, listId, "delete_role_list");
                if (!hasListPerm) {
                    throw new ForbiddenException("Non hai il permesso di eliminare ruoli in questa specifica lista.");
                }
            }
            maxAuthLevel = roleRepository.findMaxLevelByUserIdAndListId(authUserId, listId);
        }

        // 5. Controllo Gerarchico Finale
        if (roleTarget.getLevel() >= maxAuthLevel) {
            throw new ForbiddenException(String.format(
                    "Violazione gerarchica: non puoi eliminare il ruolo '%s' (Level %d) " +
                            "perché il tuo massimo livello in questo contesto è %d.",
                    roleTarget.getName(), roleTarget.getLevel(), maxAuthLevel));
        }

        // 6. Esecuzione Eliminazione Fisica
        log.info("Eliminazione ruolo: {} (ID: {}) da parte dell'utente {}", roleTarget.getName(), roleId, authUserId);
        roleRepository.delete(roleTarget);
    }

    @Override
    @Transactional()
    public Set<RoleSummaryDto> getAllVisible(Long authUserId, @Nullable Long listId) {
        User user = userRepository.findById(authUserId)
                .orElseThrow(() -> new NotFoundException("Utente autenticato non trovato"));

        Long orgId = user.getOrg().getId();

        // 1. Controllo Permessi Base
        boolean canOrg = permissionService.hasPermission(authUserId, "view_all_role_organization");
        boolean canList = permissionService.hasPermission(authUserId, "view_all_role_list");

        if (!canOrg && !canList) {
            throw new ForbiddenException("Non hai permessi per visualizzare ruoli (Org o Lista)");
        }

        Set<Role> roles;

        // 2. CASO 1: Nessun list_id (Vista globale Org)
        if (listId == null) {
            if (!canOrg) {
                throw new ForbiddenException("È richiesto il permesso view_all_role_organization per visualizzare tutti i ruoli.");
            }
            // Recupera TUTTI i ruoli dell'Org (sia globali che di lista)
            roles = roleRepository.findAllByOrganizationId(orgId);
        }
        // 3. CASO 2: Filtro per Lista Specifica
        else {
            // Verifica esistenza e appartenenza della Lista
            List targetList = listRepository.findById(listId)
                    .orElseThrow(() -> new NotFoundException("Lista con ID " + listId + " non trovata."));

            if (!targetList.getOrg().getId().equals(orgId)) {
                throw new NotFoundException("Lista non trovata nella tua organizzazione.");
            }

            // Verifica Autorizzazione sulla lista specifica
            if (!canOrg) {
                boolean hasAccessOnList = permissionService.hasPermissionOnList(authUserId, listId, "view_all_role_list");
                if (!hasAccessOnList) {
                    throw new ForbiddenException("Non hai accesso a visualizzare i ruoli di questa lista specifica.");
                }
            }

            roles = roleRepository.findAllByListId(listId);
        }

        // 4. Conversione in DTO
        return roles.stream()
                .map(RoleSummaryDto::new)
                .collect(Collectors.toSet());
    }

    /**
     * Recupera i dettagli di un ruolo specifico o i ruoli dell'utente stesso.
     * Implementa controlli di visibilità basati su Org e List permissions.
     */
    @Override
    @Transactional()
    public RoleInfoResponse getRoleInformation(Long authUserId, Long roleId) {
        User authUser = userRepository.findById(authUserId)
                .orElseThrow(() -> new NotFoundException("Utente autenticato non trovato"));

        // Prepara i ruoli dell'utente (sempre pronti per diagnostica o se roleId è null)
        Set<RoleSummaryDto> authUserRoles = authUser.getRoles().stream()
                .map(RoleSummaryDto::new)
                .collect(Collectors.toSet());

        // CASO 1: Nessun roleId fornito -> Restituisce i propri ruoli
        if (roleId == null) {
            return new RoleInfoResponse(authUserRoles, null, authUserRoles.size());
        }

        // CASO 2: roleId fornito -> Verifica permessi
        Role targetRole = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Ruolo target non trovato"));

        // Controllo Organizzazione
        if (!targetRole.getOrganization().getId().equals(authUser.getOrg().getId())) {
            throw new ForbiddenException("Permesso negato: il ruolo appartiene a un'altra organizzazione.");
            // Nota: se vuoi includere authUserRoles nel corpo del 403,
            // dovresti gestire un'eccezione custom che trasporti i dati.
        }

        boolean permOrg = permissionService.hasPermission(authUserId, "view_all_role_organization");
        boolean permList = permissionService.hasPermission(authUserId, "view_all_role_list");

        // Autorizzazione
        boolean authorized = false;

        if (permOrg) {
            authorized = true;
        } else if (targetRole.getList() != null && permList) {
            // Verifica se ha accesso alla lista specifica del ruolo
            authorized = permissionService.hasPermissionOnList(authUserId, targetRole.getList().getId(), "view_all_role_list");
        }

        if (!authorized) {
            String msg = targetRole.getList() == null
                    ? "Permesso negato. Richiesto 'view_all_role_organization'."
                    : "Permesso negato per questa lista specifica.";
            throw new ForbiddenException(msg);
        }

        Set<RoleSummaryDto> targetRoleData = Set.of(new RoleSummaryDto(targetRole));
        return new RoleInfoResponse(targetRoleData, null, 1);
    }

    @Override
    @Transactional
    public RoleSummaryDto update(RoleUpdateDto dto, Long authUserId) {
        // 1. Recupero Utente e Ruolo Target
        User authUser = userRepository.findById(authUserId)
                .orElseThrow(() -> new NotFoundException("Utente autenticato non trovato"));

        Role roleTarget = roleRepository.findById(dto.roleId())
                .orElseThrow(() -> new NotFoundException("Ruolo non trovato"));

        // 2. Controllo Multi-tenancy
        if (!roleTarget.getOrganization().getId().equals(authUser.getOrg().getId())) {
            throw new ForbiddenException("Non puoi modificare ruoli di altre organizzazioni.");
        }

        // 3. Verifica Permessi e Calcolo Autorità (Max Level)
        boolean canOrg = permissionService.hasPermission(authUserId, "update_role_organization");
        boolean canList = permissionService.hasPermission(authUserId, "update_role_list");

        boolean isOrgRole = roleTarget.getList() == null;
        int maxAuthLevel = 0;

        if (isOrgRole) {
            if (!canOrg) throw new ForbiddenException("Permesso update_role_organization richiesto.");
            maxAuthLevel = roleRepository.findMaxLevelByUserIdAndOrgId(authUserId, authUser.getOrg().getId());
        } else {
            Long listId = roleTarget.getList().getId();
            if (!canOrg) {
                if (!canList || !permissionService.hasPermissionOnList(authUserId, listId, "update_role_list")) {
                    throw new ForbiddenException("Non hai i permessi per modificare ruoli in questa lista.");
                }
            }
            maxAuthLevel = roleRepository.findMaxLevelByUserIdAndListId(authUserId, listId);
        }

        // 4. CONTROLLO GERARCHICO (TARGET): Non puoi modificare chi è al tuo livello o sopra
        if (roleTarget.getLevel() >= maxAuthLevel) {
            throw new ForbiddenException("Non puoi modificare un ruolo di livello pari o superiore al tuo (" + maxAuthLevel + ").");
        }

        // 5. APPLICAZIONE MODIFICHE
        if (dto.name() != null) roleTarget.setName(dto.name());
        if (dto.color() != null) roleTarget.setColor(dto.color());

        // Controllo Nuovo Livello: non può superare la tua autorità
        if (dto.level() != null) {
            if (dto.level() > maxAuthLevel) {
                throw new ForbiddenException("Non puoi impostare un livello superiore al tuo massimo (" + maxAuthLevel + ").");
            }
            roleTarget.setLevel(dto.level());
        }

        // 6. AGGIORNAMENTO PERMESSI (Controllo di Possessione)
        if (dto.permissions() != null) {
            Set<Long> userPermIds = permissionService.getUserPermissions(authUserId).stream()
                    .map(PermissionSummaryDto::getId)
                    .collect(Collectors.toSet());

            if (!userPermIds.containsAll(dto.permissions())) {
                throw new ForbiddenException("Non puoi assegnare permessi che non possiedi personalmente.");
            }

            // CORREZIONE QUI:
            // 1. Recupera i permessi come java.util.List
            java.util.List<Permission> foundPerms = permissionRepository.findAllById(dto.permissions());

            // 2. Crea un nuovo HashSet passando la lista al costruttore
            Set<Permission> newPermsSet = new HashSet<>(foundPerms);

            // 3. Assegna il Set al ruolo target
            roleTarget.setPermissions(newPermsSet);
        }

        return new RoleSummaryDto(roleRepository.save(roleTarget));
    }
}