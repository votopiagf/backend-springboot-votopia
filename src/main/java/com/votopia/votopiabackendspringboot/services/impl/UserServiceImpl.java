package com.votopia.votopiabackendspringboot.services.impl;

import com.votopia.votopiabackendspringboot.dtos.user.UserCreateDto;
import com.votopia.votopiabackendspringboot.dtos.user.UserSummaryDto;
import com.votopia.votopiabackendspringboot.entities.List;
import com.votopia.votopiabackendspringboot.entities.Organization;
import com.votopia.votopiabackendspringboot.entities.Role;
import com.votopia.votopiabackendspringboot.entities.User;
import com.votopia.votopiabackendspringboot.exceptions.ConflictException;
import com.votopia.votopiabackendspringboot.exceptions.NotFoundException;
import com.votopia.votopiabackendspringboot.repositories.ListRepository;
import com.votopia.votopiabackendspringboot.repositories.OrganizationRepository;
import com.votopia.votopiabackendspringboot.repositories.RoleRepository;
import com.votopia.votopiabackendspringboot.repositories.UserRepository;
import com.votopia.votopiabackendspringboot.services.UserService;
import com.votopia.votopiabackendspringboot.services.PermissionService;
import com.votopia.votopiabackendspringboot.exceptions.ForbiddenException;
import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ListRepository listRepository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private  PermissionService permissionService; // Ora è iniettato correttamente

    @Override
    @Transactional
    public UserSummaryDto register(UserCreateDto dto, Long authUserId) {
        log.info("Inizio registrazione nuovo utente: {} da parte di admin ID: {}", dto.getEmail(), authUserId);

        // 1. Recupero utente autenticato
        User authUser = userRepository.findById(authUserId)
                .orElseThrow(() -> new NotFoundException("Utente autenticato non trovato"));

        // 2. Controllo Permessi
        boolean canOrg = permissionService.hasPermission(authUserId, "create_user_for_organization");
        boolean canList = permissionService.hasPermission(authUserId, "create_user_for_list");

        if (!canOrg && !canList) {
            throw new ForbiddenException("Non hai i permessi per creare utenti");
        }
        boolean existingUser = userRepository.existsByEmailAndOrgAndDeletedFalse(dto.getEmail(), authUser.getOrg());
        if (existingUser) throw new ConflictException("Utente con questa email già esiste nell'organizzazione");

        // 3. Creazione Utente Base
        User newUser = new User();
        newUser.setName(dto.getName());
        newUser.setSurname(dto.getSurname());
        newUser.setEmail(dto.getEmail());
        newUser.setPassword(passwordEncoder.encode(dto.getPassword()));
        newUser.setOrg(authUser.getOrg());

        // Salviamo una prima volta per generare l'ID necessario alle relazioni
        newUser = userRepository.save(newUser);

        // 4. Gestione Liste (Caso Org vs Caso List)
        if (canOrg) {
            processOrgLists(dto.getListsId(), newUser, authUser.getOrg().getId());
        } else {
            processRestrictedList(dto.getListsId(), newUser, authUser);
        }

        // 5. Assegnazione Ruoli con controllo Livello (Hierarchy)
        processRoles(dto.getRolesId(), newUser, authUser);

        // Salvataggio finale con relazioni
        return new UserSummaryDto(userRepository.save(newUser));
    }

    private void processOrgLists(Set<Long> listIds, User newUser, Long orgId) {
        if (listIds == null) return;
        for (Long id : listIds) {
            // Specifichiamo il tipo completo qui per non lasciare dubbi al compilatore
            Optional<List> optList =
                    listRepository.findByIdAndOrgId(id, orgId);

            if (optList.isPresent()) {
                com.votopia.votopiabackendspringboot.entities.List targetList = optList.get();

                // Inizializzazione di sicurezza se il Set è null
                if (targetList.getUsers() == null) {
                    targetList.setUsers(new java.util.HashSet<>());
                }

                targetList.getUsers().add(newUser);
                // Salva le modifiche
                listRepository.save(targetList);
            }
        }
    }

    private void processRestrictedList(Set<Long> listIds, User newUser, User authUser) {
        if (listIds == null || listIds.size() != 1) {
            throw new ForbiddenException("Puoi creare utenti in una sola lista alla volta");
        }

        Long targetId = listIds.iterator().next();

        boolean hasPermInList = authUser.getRoles().stream()
                .anyMatch(r -> r.getList() != null &&
                        r.getList().getId().equals(targetId) &&
                        r.getPermissions().stream().anyMatch(p -> p.getName().equals("create_user_for_list")));

        if (!hasPermInList) {
            throw new ForbiddenException("Non hai permessi su questa lista");
        }

        listRepository.findById(targetId).ifPresent(l -> l.getUsers().add(newUser));
    }

    private void processRoles(Set<Long> roleIds, User newUser, User authUser) {
        if (roleIds == null) return;
        for (Long roleId : roleIds) {
            Role role = roleRepository.findById(roleId).orElse(null);
            if (role == null) continue;

            if (role.isOrgLevel()) {
                int maxLevel = authUser.getRoles().stream()
                        .filter(Role::isOrgLevel)
                        .mapToInt(Role::getLevel).max().orElse(0);
                if (role.getLevel() <= maxLevel) newUser.getRoles().add(role);
            } else if (role.getList() != null) {
                int maxLevel = authUser.getRoles().stream()
                        .filter(r -> r.getList() != null && r.getList().equals(role.getList()))
                        .mapToInt(Role::getLevel).max().orElse(0);
                if (role.getLevel() <= maxLevel) newUser.getRoles().add(role);
            }
        }
    }

    @Override
    public UserSummaryDto getUserInformation(Long authUserId, Long targetUserId) {
        log.info("Richiesta informazioni utente. Richiedente ID: {}, Target ID: {}", authUserId, targetUserId);

        // 1. Recupero l'utente autenticato
        User authUser = userRepository.findById(authUserId)
                .orElseThrow(() -> {
                    log.error("Errore: Utente richiedente {} non trovato nel database", authUserId);
                    return new NotFoundException("Utente autenticato non trovato");
                });

        // 2. Se non c'è targetUserId o è se stesso
        if (targetUserId == null || targetUserId.equals(authUserId)) {
            log.info("Visualizzazione profilo personale (self) per l'utente: {}", authUserId);
            return new UserSummaryDto(authUser);
        }

        // 3. Recupero l'utente target
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> {
                    log.warn("Tentativo di visualizzare utente non esistente: {}", targetUserId);
                    return new NotFoundException("Utente target non trovato");
                });

        // 4. Controllo Organizzazione
        if (!targetUser.getOrg().getId().equals(authUser.getOrg().getId())) {
            log.warn("VIOLAZIONE SICUREZZA: L'utente {} (Org: {}) ha tentato di visualizzare l'utente {} (Org: {})",
                    authUserId, authUser.getOrg().getId(), targetUserId, targetUser.getOrg().getId());
            throw new ForbiddenException("Non puoi vedere utenti di altre organizzazioni");
        }

        // 5. Controllo Permessi
        boolean permOrg = permissionService.hasPermission(authUserId, "view_all_user_organization");
        boolean permLists = permissionService.hasPermission(authUserId, "view_all_user_list");

        log.debug("Permessi utente {}: view_org={}, view_lists={}", authUserId, permOrg, permLists);

        if (permOrg) {
            log.info("Accesso consentito a ID {} tramite permesso ORGANIZATION", targetUserId);
            return new UserSummaryDto(targetUser);
        }

        if (permLists) {
            boolean isSharedList = targetUser.getLists().stream()
                    .anyMatch(list -> {
                        boolean hasPerm = permissionService.hasPermissionOnList(authUserId, list.getId(), "view_all_user_list");
                        if (hasPerm) log.debug("Permesso trovato sulla lista ID: {}", list.getId());
                        return hasPerm;
                    });

            if (!isSharedList) {
                log.warn("Accesso negato: L'utente {} ha il permesso LIST ma non condivide liste autorizzate con l'utente {}", authUserId, targetUserId);
                throw new ForbiddenException("Permesso negato per visualizzare questo utente nelle tue liste");
            }

            log.info("Accesso consentito a ID {} tramite permesso LIST", targetUserId);
            return new UserSummaryDto(targetUser);
        }

        // Se arriva qui, non ha permessi sufficienti
        log.warn("Accesso negato: L'utente {} non ha permessi sufficienti per vedere l'utente {}", authUserId, targetUserId);
        throw new ForbiddenException("Permesso negato per visualizzare questo utente");
    }

    @Override
    public Set<UserSummaryDto> getAllVisibleUsers(Long authUserId, @Nullable Long listId) {
        // 1. Recupero utente autenticato
        User authUser = userRepository.findById(authUserId)
                .orElseThrow(() -> new NotFoundException("Utente autenticato non trovato"));

        Long orgId = authUser.getOrg().getId();
        log.info("Verifica permessi per utente {} nell'organizzazione {}", authUserId, orgId);

        // 2. Recupero permessi
        boolean hasOrgPerm = permissionService.hasPermission(authUserId, "view_all_user_organization");
        boolean hasListPerm = permissionService.hasPermission(authUserId, "view_all_user_list");

        // CASO A: Nessun listId fornito -> Richiede permesso Organizzazione
        if (listId == null) {
            if (!hasOrgPerm) {
                log.warn("Accesso negato: l'utente {} ha tentato di vedere tutta l'organizzazione senza permesso", authUserId);
                throw new ForbiddenException("Non hai il permesso per vedere tutti gli utenti dell'organizzazione");
            }

            log.info("Recupero tutti gli utenti per l'organizzazione: {}", orgId);
            return userRepository.findAllByOrgId(orgId).stream()
                    .map(UserSummaryDto::new)
                    .collect(Collectors.toSet());
        }

        // CASO B: listId fornito -> Richiede permesso List + appartenenza/potere su quella lista
        if (!hasListPerm) {
            log.warn("Accesso negato: l'utente {} ha tentato di filtrare per lista senza permesso view_all_user_list", authUserId);
            throw new ForbiddenException("Non hai il permesso per vedere utenti di liste specifiche");
        }

        // Verifica se l'admin ha potere su QUELLA specifica lista
        boolean canAccessThisList = permissionService.hasPermissionOnList(authUserId, listId, "view_all_user_list");

        if (!canAccessThisList) {
            log.warn("Accesso negato: l'utente {} non ha accesso alla lista specifica {}", authUserId, listId);
            throw new ForbiddenException("Non hai accesso agli utenti di questa lista");
        }

        log.info("Recupero utenti per la lista: {}", listId);
        // Nota: findAllByListsId deve essere definito nel UserRepository
        return userRepository.findAllByListsId(listId).stream()
                .map(UserSummaryDto::new)
                .collect(Collectors.toSet());
    }

    @Override
    public void delete(Long authUserId, Long targetUserId) {
        // 1. Recupero utente autenticato
        User authUser = userRepository.findById(authUserId)
                .orElseThrow(() -> new NotFoundException("Utente autenticato non trovato"));

        // 2. Controllo permesso 'delete_user_organization'
        boolean hasPerm = permissionService.hasPermission(authUserId, "delete_user_organization");
        if (!hasPerm) {
            log.warn("Accesso negato: l'utente {} non ha il permesso delete_user_organization", authUserId);
            throw new ForbiddenException("Permesso negato");
        }

        // 3. Recupero utente da eliminare
        User userToDelete = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("Utente da eliminare non trovato"));

        // 4. Verifica organizzazione (cross-org check)
        if (!userToDelete.getOrg().getId().equals(authUser.getOrg().getId())) {
            log.warn("VIOLAZIONE SICUREZZA: L'utente {} ha tentato di eliminare l'utente {} di un'altra Org",
                    authUserId, targetUserId);
            throw new ForbiddenException("Non puoi eliminare utenti di altre organizzazioni");
        }

        // 5. Esecuzione Soft Delete
        log.info("Esecuzione soft delete per l'utente ID: {}", targetUserId);
        userToDelete.setDeleted(true);

        // Non serve chiamare save() esplicitamente se il metodo è @Transactional,
        // ma lo mettiamo per chiarezza o se non usi il dirty checking automatico.
        userRepository.save(userToDelete);
    }
}