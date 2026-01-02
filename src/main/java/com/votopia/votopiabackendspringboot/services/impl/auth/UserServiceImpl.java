package com.votopia.votopiabackendspringboot.services.impl.auth;

import com.votopia.votopiabackendspringboot.dtos.user.UserCreateDto;
import com.votopia.votopiabackendspringboot.dtos.user.UserDetailDto;
import com.votopia.votopiabackendspringboot.dtos.user.UserSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.user.UserUpdateDto;
import com.votopia.votopiabackendspringboot.entities.auth.Role;
import com.votopia.votopiabackendspringboot.entities.auth.User;
import com.votopia.votopiabackendspringboot.entities.lists.List;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
    public UserDetailDto getUserInformation(Long authUserId, @Nullable Long targetUserId) {
        UserDetailDto result;
        User authUser = authService.getAuthenticatedUser(authUserId);

        // Profilo personale
        if (targetUserId == null || targetUserId.equals(authUserId)) {
            result = new UserDetailDto(authUser);
        } else {
            User targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new NotFoundException("Utente target non trovato"));// Multi-tenancy check
            if (!targetUser.getOrg().getId().equals(authUser.getOrg().getId())) {
                throw new ForbiddenException("Non puoi vedere utenti di altre organizzazioni");
            }// Controllo Permessi
            boolean permOrg = permissionService.hasPermission(authUserId, "view_all_user_organization");
            if (permOrg) {
                result = new UserDetailDto(targetUser);
            } else {
                boolean permLists = targetUser.getLists().stream()
                        .anyMatch(list -> permissionService.hasPermissionOnList(authUserId, list.getId(), "view_all_user_list"));
                if (!permLists) {
                    throw new ForbiddenException("Permesso negato per visualizzare questo utente");
                }
                result = new UserDetailDto(targetUser);
            }
        }

        return result;
    }

    @Override
    @Transactional
    public Set<UserDetailDto> getAllVisibleUsers(Long authUserId, @Nullable Long listId) {
        User authUser = authService.getAuthenticatedUser(authUserId);
        Long orgId = authUser.getOrg().getId();

        if (listId == null) {
            if (!permissionService.hasPermission(authUserId, "view_all_user_organization")) {
                throw new ForbiddenException("Permesso richiesto per vedere tutti gli utenti dell'organizzazione");
            }
            return userRepository.findAllByOrgId(orgId).stream()
                    .map(UserDetailDto::new).collect(Collectors.toSet());
        }

        if (!permissionService.hasPermissionOnList(authUserId, listId, "view_all_user_list")) {
            throw new ForbiddenException("Non hai accesso agli utenti di questa lista");
        }

        return userRepository.findAllByListsId(listId).stream()
                .map(UserDetailDto::new).collect(Collectors.toSet());
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
    public void deleteList(Set<Long> idTargets, Long authUserId) {

        User authUser = authService.getAuthenticatedUser(authUserId);

        if (!permissionService.hasPermission(authUserId, "delete_user_organization")) {
            throw new ForbiddenException("Permesso di eliminazione richiesto");
        }

        Long orgId = authUser.getOrg().getId();

        // 1 Fetch batch
        Set<User> usersToDelete = new HashSet<>(userRepository.findAllById(idTargets));

        if (usersToDelete.size() != idTargets.size()) {
            throw new NotFoundException("Uno o più utenti da eliminare non sono stati trovati");
        }

        // 2 Check organizzazione
        boolean allInSameOrg = usersToDelete.stream()
                .allMatch(u -> u.getOrg().getId().equals(orgId));

        if (!allInSameOrg) {
            throw new ForbiddenException("Non puoi eliminare utenti di altre organizzazioni");
        }

        // 3 Soft delete
        usersToDelete.forEach(user -> user.setDeleted(true));

        // 4 Save batch
        userRepository.saveAll(usersToDelete);
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

    @Override
    @Transactional
    public Set<UserSummaryDto> updateListUsers(Set<UserUpdateDto> users, Long authUserId) {

        User authUser = authService.getAuthenticatedUser(authUserId);

        boolean canOrg  = permissionService.hasPermission(authUserId, "update_user_organization");
        boolean canList = permissionService.hasPermission(authUserId, "update_user_list");

        if (!canOrg && !canList) {
            throw new ForbiddenException("Non hai i permessi per modificare gli utenti");
        }

        Long orgId = authUser.getOrg().getId();

        // 1 Recupero tutti gli ID una sola volta
        Set<Long> userIds = users.stream()
                .map(UserUpdateDto::id)
                .collect(Collectors.toSet());

        // 2 Fetch batch degli utenti
        Set<User> dbUsers = new HashSet<>(userRepository.findAllById(userIds));

        if (dbUsers.size() != userIds.size()) {
            throw new NotFoundException("Qualche utente non è stato trovato");
        }

        // 3 Verifica che siano tutti nella stessa org
        boolean allInOrg = dbUsers.stream()
                .allMatch(u -> u.getOrg().getId().equals(orgId));

        if (!allInOrg) {
            throw new NotFoundException("Utenti non appartengono all'organizzazione");
        }

        // 4 Map per lookup O(1)
        Map<Long, User> userMap = dbUsers.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // 5 Update
        for (UserUpdateDto dto : users) {
            User targetUser = userMap.get(dto.id());

            if (dto.name() != null) {
                targetUser.setName(dto.name());
            }

            if (dto.surname() != null) {
                targetUser.setSurname(dto.surname());
            }

            if (dto.email() != null &&
                    !dto.email().equals(targetUser.getEmail()) &&
                    !userRepository.existsByEmailAndOrgAndDeletedFalse(dto.email(), authUser.getOrg())) {

                targetUser.setEmail(dto.email());
            }

            if (Boolean.TRUE.equals(dto.resetPassword())) {
                targetUser.setPassword(passwordEncoder.encode("Cambiami"));
                targetUser.setMustChangePassword(true);
            }

            handleListUpdate(targetUser, authUserId, dto, canOrg);
        }

        // 6 Save batch
        return userRepository.saveAll(dbUsers)
                .stream()
                .map(UserSummaryDto::new)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public Set<UserSummaryDto> registerListUsers(Set<UserCreateDto> users, Long authUserId) {
        User authUser = authService.getAuthenticatedUser(authUserId);

        // 1 Controllo Permessi
        boolean canOrg = permissionService.hasPermission(authUserId, "create_user_for_organization");
        boolean canList = permissionService.hasPermission(authUserId, "create_user_for_list");

        if (!canOrg && !canList) {
            throw new ForbiddenException("Non hai i permessi per creare utenti");
        }

        // 2 Mappa email -> DTO per lookup rapido
        Map<String, UserCreateDto> dtoByEmail = users.stream()
                .collect(Collectors.toMap(UserCreateDto::email, Function.identity()));

        // 3 Controllo email già esistenti
        Set<String> existingEmails = dtoByEmail.keySet().stream()
                .filter(email -> userRepository.findUserByEmailAndOrg(email, authUser.getOrg()).isPresent())
                .collect(Collectors.toSet());

        if (!existingEmails.isEmpty()) {
            throw new ConflictException(
                    "Esistono degli utenti per queste email: " + String.join(", ", existingEmails)
            );
        }

        // 4 Creazione utenti e salvataggio batch
        Set<User> usersToSave = dtoByEmail.values().stream()
                .map(dto -> {
                    User u = new User();
                    u.setName(dto.name());
                    u.setSurname(dto.surname());
                    u.setEmail(dto.email());
                    u.setPassword(passwordEncoder.encode(dto.password()));
                    u.setOrg(authUser.getOrg());
                    return u;
                })
                .collect(Collectors.toSet());

        Set<User> usersSaved = new HashSet<>(userRepository.saveAll(usersToSave));

        // 5 Gestione liste e ruoli per ogni utente salvato
        usersSaved.forEach(savedUser -> {
            UserCreateDto dto = dtoByEmail.get(savedUser.getEmail());

            if (canOrg) {
                processOrgLists(dto.listsId(), savedUser, authUser.getOrg().getId());
            } else {
                processRestrictedList(dto.listsId(), savedUser, authUser.getId());
            }

            processRoles(dto.rolesId(), savedUser, authUser.getId());
        });

        // 6 Restituisci DTO
        return usersSaved.stream()
                .map(UserSummaryDto::new)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream createExcelAllVisibleUsers(Long authUserId, @Nullable Long targetListId) {
        User authUser = authService.getAuthenticatedUser(authUserId);
        Set<UserDetailDto> users = this.getAllVisibleUsers(authUserId, targetListId);

        String orgName = authUser.getOrg().getName();
        String listName = "";
        if (targetListId != null) {
            listName = listRepository.findById(targetListId)
                    .map(l -> l.getName()).orElse("");
        }

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Lista Utenti");

            // --- 1. DEFINIZIONE STILI ---

            // Stile Titolo (In alto)
            CellStyle titleStyle = wb.createCellStyle();
            Font titleFont = wb.createFont();
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setBold(true);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            // Stile Header (Le colonne)
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex()); // Blu professionale
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(headerStyle); // Metodo helper per i bordi

            // Stile Dati (Celle normali)
            CellStyle dataStyle = wb.createCellStyle();
            setBorders(dataStyle);
            dataStyle.setAlignment(HorizontalAlignment.LEFT);

            // --- 2. COSTRUZIONE DEL FOGLIO ---

            // RIGA 0: Titolo Principale
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Report Utenti: " + orgName + (listName.isEmpty() ? "" : " - " + listName));
            titleCell.setCellStyle(titleStyle);
            // Uniamo le celle (da colonna 0 a 2)
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

            // RIGA 2: Intestazioni (lasciamo una riga vuota tra titolo e tabella)
            Row headerRow = sheet.createRow(2);
            String[] columns = {"COGNOME", "NOME", "EMAIL"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // RIGA 3+: Dati
            int rowIdx = 3;
            for (UserDetailDto user : users) {
                Row row = sheet.createRow(rowIdx++);

                Cell cell0 = row.createCell(0);
                cell0.setCellValue(user.surname());
                cell0.setCellStyle(dataStyle);

                Cell cell1 = row.createCell(1);
                cell1.setCellValue(user.name());
                cell1.setCellStyle(dataStyle);

                Cell cell2 = row.createCell(2);
                cell2.setCellValue(user.email());
                cell2.setCellStyle(dataStyle);
            }

            // 3. Auto-size delle colonne
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException("Errore generazione Excel", e);
        }
    }

    // --- HELPERS ---

    // Metodo helper per aggiungere bordi neri sottili
    private void setBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
    }

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