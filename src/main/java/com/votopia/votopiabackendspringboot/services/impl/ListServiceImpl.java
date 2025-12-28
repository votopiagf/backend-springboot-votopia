package com.votopia.votopiabackendspringboot.services.impl;

import com.votopia.votopiabackendspringboot.dtos.list.ListCreateDto;
import com.votopia.votopiabackendspringboot.dtos.list.ListSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.list.ListUpdateDto;
import com.votopia.votopiabackendspringboot.entities.lists.List;
import com.votopia.votopiabackendspringboot.entities.organizations.Organization;
import com.votopia.votopiabackendspringboot.entities.auth.User;
import com.votopia.votopiabackendspringboot.exceptions.BadRequestException;
import com.votopia.votopiabackendspringboot.exceptions.ConflictException;
import com.votopia.votopiabackendspringboot.exceptions.ForbiddenException;
import com.votopia.votopiabackendspringboot.exceptions.NotFoundException;
import com.votopia.votopiabackendspringboot.repositories.lists.ListRepository;
import com.votopia.votopiabackendspringboot.services.ListService;
import com.votopia.votopiabackendspringboot.services.auth.AuthService;
import com.votopia.votopiabackendspringboot.services.auth.PermissionService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementazione del servizio ListService.
 * Utilizza AuthService per il recupero dell'utente e PermissionService per la sicurezza.
 */
@Slf4j
@Service
public class ListServiceImpl implements ListService {

    @Autowired private ListRepository listRepository;
    @Autowired private AuthService authService; // Centralizzato per recupero utente
    @Autowired private PermissionService permissionService;

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6})$");

    @Override
    @Transactional
    public ListSummaryDto create(ListCreateDto dto, Long authUserId) {
        User authUser = authService.getAuthenticatedUser(authUserId);
        Organization org = authUser.getOrg();

        if (!permissionService.hasPermission(authUserId, "create_list")) {
            throw new ForbiddenException("Permesso 'create_list' richiesto.");
        }

        // Controllo Limite Massimo Liste (Business Logic dell'Org)
        if (org.getMaxLists() != null) {
            long currentListsCount = listRepository.countByOrgId(org.getId());
            if (currentListsCount >= org.getMaxLists()) {
                throw new ForbiddenException("L'Organizzazione ha raggiunto il limite massimo di " + org.getMaxLists() + " liste.");
            }
        }

        validateColor(dto.colorPrimary(), "primario");
        validateColor(dto.colorSecondary(), "secondario");

        List newList = new List();
        newList.setName(dto.name().trim());
        newList.setOrg(org);
        newList.setDescription(dto.description() != null ? dto.description().trim() : "");
        newList.setSlogan(dto.slogan() != null ? dto.slogan().trim() : "");
        newList.setColorPrimary(dto.colorPrimary() != null ? dto.colorPrimary().toUpperCase() : null);
        newList.setColorSecondary(dto.colorSecondary() != null ? dto.colorSecondary().toUpperCase() : null);

        try {
            List savedList = listRepository.save(newList);
            log.info("Nuova lista creata: {} per l'Org: {}", savedList.getName(), org.getName());
            return new ListSummaryDto(savedList);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Una lista con questo nome esiste già nella tua Organizzazione.");
        }
    }

    @Override
    @Transactional
    public ListSummaryDto update(ListUpdateDto dto, Long authUserId) {
        User authUser = authService.getAuthenticatedUser(authUserId);

        List listTarget = listRepository.findById(dto.listId())
                .orElseThrow(() -> new NotFoundException("Lista non trovata"));

        // Multi-tenancy check
        if (!listTarget.getOrg().getId().equals(authUser.getOrg().getId())) {
            throw new ForbiddenException("Accesso negato: la lista appartiene a un'altra Organizzazione.");
        }

        // Controllo permessi (Org-wide o List-specific)
        boolean hasAccess = permissionService.hasPermission(authUserId, "update_list_organization") ||
                permissionService.hasPermissionOnList(authUserId, dto.listId(), "update_list_list");

        if (!hasAccess) throw new ForbiddenException("Non hai i permessi necessari per modificare questa lista.");

        // Update fields
        if (dto.name() != null) listTarget.setName(dto.name().trim());
        if (dto.description() != null) listTarget.setDescription(dto.description().trim());
        if (dto.slogan() != null) listTarget.setSlogan(dto.slogan().trim());

        if (dto.colorPrimary() != null) {
            validateColor(dto.colorPrimary(), "primario");
            listTarget.setColorPrimary(dto.colorPrimary().toUpperCase());
        }
        if (dto.colorSecondary() != null) {
            validateColor(dto.colorSecondary(), "secondario");
            listTarget.setColorSecondary(dto.colorSecondary().toUpperCase());
        }

        try {
            List savedList = listRepository.save(listTarget);
            log.info("Lista aggiornata: {} (ID: {})", savedList.getName(), savedList.getId());
            return new ListSummaryDto(savedList);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Una lista con questo nome esiste già nella tua Organizzazione.");
        }
    }

    @Override
    @Transactional
    public Set<ListSummaryDto> getAllVisibleLists(Long authUserId) {
        User authUser = authService.getAuthenticatedUser(authUserId);
        Long orgId = authUser.getOrg().getId();

        boolean canViewAll = permissionService.hasPermission(authUserId, "view_all_lists");

        Set<List> lists = canViewAll ?
                listRepository.findAllByOrgId(orgId) :
                listRepository.findAllByUsersIdAndOrgId(authUserId, orgId);

        return lists.stream()
                .map(ListSummaryDto::new)
                .collect(Collectors.toSet());
    }

    // --- UTILITY METHODS ---

    private void validateColor(String color, String type) {
        if (color != null && !HEX_COLOR_PATTERN.matcher(color).matches()) {
            throw new BadRequestException("Formato colore " + type + " non valido. Deve essere #RRGGBB.");
        }
    }
}