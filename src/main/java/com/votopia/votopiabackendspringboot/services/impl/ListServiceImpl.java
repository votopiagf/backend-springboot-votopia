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
import com.votopia.votopiabackendspringboot.repositories.auth.UserRepository;
import com.votopia.votopiabackendspringboot.services.ListService;
import com.votopia.votopiabackendspringboot.services.auth.PermissionService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ListServiceImpl implements ListService {

    @Autowired
    private ListRepository listRepository;
    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private final PermissionService permissionService;

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6})$");

    public ListServiceImpl(ListRepository listRepository, UserRepository userRepository, PermissionService permissionService) {
        this.listRepository = listRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
    }

    @Override
    @Transactional
    public ListSummaryDto create(ListCreateDto dto, Long authUserId) {
        // 1. Autenticazione e Utente
        User authUser = userRepository.findById(authUserId)
                .orElseThrow(() -> new NotFoundException("Utente autenticato non trovato"));

        if (authUser.getOrg() == null) {
            throw new ForbiddenException("L'utente non è associato ad alcuna Organizzazione");
        }

        Organization org = authUser.getOrg();

        // 2. Controllo Permesso
        if (!permissionService.hasPermission(authUserId, "create_list")) {
            throw new ForbiddenException("Permesso 'create_list' richiesto.");
        }

        // 3. Controllo Limite Massimo Liste
        if (org.getMaxLists() != null) {
            long currentListsCount = listRepository.countByOrgId(org.getId());
            if (currentListsCount >= org.getMaxLists()) {
                throw new ForbiddenException("L'Organizzazione ha raggiunto il limite massimo di " + org.getMaxLists() + " liste.");
            }
        }

        // 4. Validazione Colori
        validateColor(dto.colorPrimary(), "primario");
        validateColor(dto.colorSecondary(), "secondario");

        // 5. Creazione Entità
        List newList = new List();
        newList.setName(dto.name().trim());
        newList.setOrg(org);
        newList.setDescription(dto.description() != null ? dto.description().trim() : "");
        newList.setSlogan(dto.slogan() != null ? dto.slogan().trim() : "");
        newList.setColorPrimary(dto.colorPrimary() != null ? dto.colorPrimary().toUpperCase() : null);
        newList.setColorSecondary(dto.colorSecondary() != null ? dto.colorSecondary().toUpperCase() : null);

        // Assumiamo che ci sia un FileRepository o simili per il logo se necessario
        // newList.setLogoFileId(dto.logoFileId()); 

        try {
            List savedList = listRepository.save(newList);
            log.info("Nuova lista creata: {} per l'Org: {}", savedList.getName(), org.getName());
            return new ListSummaryDto(savedList);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Una lista con questo nome esiste già nella tua Organizzazione.");
        }
    }

    private void validateColor(String color, String type) {
        if (color != null && !HEX_COLOR_PATTERN.matcher(color).matches()) {
            throw new BadRequestException("Formato colore " + type + " non valido. Deve essere #RRGGBB.");
        }
    }

    @Override
    @Transactional
    public ListSummaryDto update(ListUpdateDto dto, Long authUserId) {
        // 1. Recupero Utente e Lista Target
        User authUser = userRepository.findById(authUserId)
                .orElseThrow(() -> new NotFoundException("Utente autenticato non trovato"));

        List listTarget = listRepository.findById(dto.listId())
                .orElseThrow(() -> new NotFoundException("Lista con ID " + dto.listId() + " non trovata"));

        // 2. Controllo Organizzazione (Multi-tenancy)
        if (!listTarget.getOrg().getId().equals(authUser.getOrg().getId())) {
            throw new ForbiddenException("Accesso negato: la lista appartiene a un'altra Organizzazione.");
        }

        // 3. Verifica Autorizzazione
        boolean canModifyOrg = permissionService.hasPermission(authUserId, "update_list_organization");
        boolean canModifySpecificList = permissionService.hasPermissionOnList(authUserId, dto.listId(), "update_list_list");

        if (!canModifyOrg && !canModifySpecificList) {
            throw new ForbiddenException("Non hai i permessi necessari per modificare questa lista.");
        }

        // 4. Applicazione Aggiornamenti
        if (dto.name() != null) listTarget.setName(dto.name().trim());
        if (dto.description() != null) listTarget.setDescription(dto.description().trim());
        if (dto.slogan() != null) listTarget.setSlogan(dto.slogan().trim());

        // Validazione Colori (Utilizziamo il metodo privato creato precedentemente)
        if (dto.colorPrimary() != null) {
            listTarget.setColorPrimary(dto.colorPrimary());
        }
        if (dto.colorSecondary() != null) {
            listTarget.setColorSecondary(dto.colorSecondary());
        }

        // Gestione Logo
        if (dto.logoFileId() != null) {
            // Implementazione recupero File se necessario
            // listTarget.setLogoFile(fileRepository.findById(dto.logoFileId()).orElse(null));
        }

        // 5. Salvataggio e gestione conflitti (Unique Constraints)
        try {
            List savedList = listRepository.save(listTarget);
            log.info("Lista aggiornata: {} (ID: {})", savedList.getName(), savedList.getId());
            return new ListSummaryDto(savedList);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Una lista con questo nome esiste già nella tua Organizzazione.");
        }
    }

    @Override
    @Transactional()
    public Set<ListSummaryDto> getAllVisibleLists(Long authUserId) {
        // 1. Recupero Utente e Org
        User authUser = userRepository.findById(authUserId)
                .orElseThrow(() -> new NotFoundException("Utente autenticato non trovato"));

        if (authUser.getOrg() == null) {
            throw new ForbiddenException("Utente non associato ad alcuna Organizzazione valida");
        }

        Long orgId = authUser.getOrg().getId();

        // 2. Controllo Permesso Globale
        boolean canViewAll = permissionService.hasPermission(authUserId, "view_all_lists");

        Set<List> lists;

        // 3. Logica di Visibilità
        if (canViewAll) {
            // Caso 1: L'utente ha il permesso di vedere ogni lista dell'organizzazione
            lists = listRepository.findAllByOrgId(orgId);
        } else {
            // Caso 2: L'utente vede solo le liste in cui è presente (relazione Many-to-Many)
            lists = listRepository.findAllByUsersIdAndOrgId(authUserId, orgId);
        }

        // 4. Mappatura in DTO
        return lists.stream()
                .map(ListSummaryDto::new)
                .collect(Collectors.toSet());
    }
}