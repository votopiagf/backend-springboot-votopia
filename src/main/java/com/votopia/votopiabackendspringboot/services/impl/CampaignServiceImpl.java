package com.votopia.votopiabackendspringboot.services.impl;

import com.votopia.votopiabackendspringboot.dtos.campaign.CampaignAddCandidateDto;
import com.votopia.votopiabackendspringboot.dtos.campaign.CampaignCreateDto;
import com.votopia.votopiabackendspringboot.dtos.campaign.CampaignSummaryDto;
import com.votopia.votopiabackendspringboot.entities.auth.User;
import com.votopia.votopiabackendspringboot.entities.campaigns.Campaign;
import com.votopia.votopiabackendspringboot.entities.campaigns.Candidate;
import com.votopia.votopiabackendspringboot.entities.campaigns.CandidateCampaign;
import com.votopia.votopiabackendspringboot.entities.campaigns.CandidatePositionCampaign;
import com.votopia.votopiabackendspringboot.entities.lists.List;
import com.votopia.votopiabackendspringboot.exceptions.BadRequestException;
import com.votopia.votopiabackendspringboot.exceptions.ConflictException;
import com.votopia.votopiabackendspringboot.exceptions.ForbiddenException;
import com.votopia.votopiabackendspringboot.exceptions.NotFoundException;
import com.votopia.votopiabackendspringboot.repositories.auth.UserRepository;
import com.votopia.votopiabackendspringboot.repositories.campaigns.*;
import com.votopia.votopiabackendspringboot.repositories.lists.ListRepository;
import com.votopia.votopiabackendspringboot.services.CampaignService;
import com.votopia.votopiabackendspringboot.services.auth.PermissionService;
import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CampaignServiceImpl implements CampaignService {

    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired private ListRepository listRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PermissionService permissionService;
    @Autowired private CandidateRepository candidateRepository;
    @Autowired private CandidatePositionCampaignRepository candidatePositionCampaignRepository;
    @Autowired private PositionRepository positionRepository;
    @Autowired private CandidateCampaignRepository candidateCampaignRepository;

    @Override
    @Transactional
    public CampaignSummaryDto create(CampaignCreateDto dto, Long authUserId) {
        // 1. Recupero Utente e Org
        User authUser = userRepository.findById(authUserId)
                .orElseThrow(() -> new NotFoundException("Utente autenticato non trovato"));

        if (authUser.getOrg() == null) {
            throw new ForbiddenException("Utente non associato ad alcuna Organizzazione");
        }

        // 2. Validazione Lista Target (deve appartenere alla stessa Org dell'utente)
        List listTarget = listRepository
                .findByIdAndOrgId(dto.listId(), authUser.getOrg().getId())
                .orElseThrow(() -> new NotFoundException("Lista target non trovata nella tua Organizzazione"));

        // 3. Validazione Date
        if (dto.startDate().isAfter(dto.endDate()) || dto.startDate().isEqual(dto.endDate())) {
            throw new BadRequestException("La data di inizio deve essere precedente alla data di fine.");
        }

        // 4. Controllo Permessi
        boolean canOrg = permissionService.hasPermission(authUserId, "create_campaign_organization");
        boolean canList = permissionService.hasPermissionOnList(authUserId, dto.listId(), "create_campaign_list");

        if (!canOrg && !canList) {
            throw new ForbiddenException("Non hai il permesso sufficiente per creare campagne in questa Lista.");
        }

        // 5. Creazione Entità
        Campaign campaign = new Campaign();
        campaign.setName(dto.name().trim());
        campaign.setDescription(dto.description());
        campaign.setStartDate(dto.startDate());
        campaign.setEndDate(dto.endDate());
        campaign.setList(listTarget);

        try {
            Campaign saved = campaignRepository.save(campaign);
            log.info("Campagna creata: {} per la lista: {}", saved.getName(), listTarget.getName());
            return new CampaignSummaryDto(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Errore di integrità: una campagna con questo nome potrebbe già esistere.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Set<CampaignSummaryDto> getAll(@Nullable Long listId, Long authUserId) {
        // 1. Recupero Utente e validazione Org
        User authUser = userRepository.findById(authUserId)
                .orElseThrow(() -> new NotFoundException("Utente autenticato non trovato"));

        if (authUser.getOrg() == null) {
            throw new ForbiddenException("Utente non associato ad alcuna Organizzazione");
        }

        Long orgId = authUser.getOrg().getId();

        // 2. Controllo Permessi Base
        boolean canOrg = permissionService.hasPermission(authUserId, "view_all_campaign_organization");

        // 3. Logica di recupero
        if (listId == null) {
            // Se non c'è listId, solo chi ha il permesso Org può vedere tutto
            if (canOrg) {
                return campaignRepository.findAllByListOrgId(orgId)
                        .stream().map(CampaignSummaryDto::new)
                        .collect(Collectors.toSet());
            } else {
                throw new ForbiddenException("Devi specificare una lista o avere permessi globali per vedere tutte le campagne.");
            }
        }

        // 4. Se listId è presente, validiamo che la lista appartenga all'Org
        listRepository.findByIdAndOrgId(listId, orgId)
                .orElseThrow(() -> new NotFoundException("Lista non trovata nella tua Organizzazione"));

        // 5. Controllo accesso alla lista specifica
        boolean canViewList = canOrg || permissionService.hasPermissionOnList(authUserId, listId, "view_all_campaign_list");

        if (canViewList) {
            return campaignRepository.findAllByListId(listId).stream()
                    .map(CampaignSummaryDto::new)
                    .collect(Collectors.toSet());
        } else {
            throw new ForbiddenException("Non hai i permessi per vedere le campagne di questa lista.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignSummaryDto get(Long campaignId, Long authUserId) {
        // 1. Recupero dati fondamentali
        User authUser = userRepository.findById(authUserId)
                .orElseThrow(() -> new NotFoundException("Utente autenticato non trovato"));

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("Campagna non trovata"));

        // 2. Controllo di sicurezza: l'utente deve appartenere alla stessa Org della campagna
        if (authUser.getOrg() == null || !campaign.getList().getOrg().getId().equals(authUser.getOrg().getId())) {
            throw new ForbiddenException("Accesso negato: la campagna non appartiene alla tua organizzazione");
        }

        // 3. Valutazione dei permessi (Cascata)
        boolean hasAccess =
                // Livello 1: Permesso Admin Organizzazione
                permissionService.hasPermission(authUserId, "view_all_campaign_organization") ||

                        // Livello 2: Permesso Gestore Lista
                        permissionService.hasPermissionOnList(authUserId, campaign.getList().getId(), "view_all_campaign_list") ||

                        // Livello 3: Partecipazione come Candidato
                        campaign.getCandidateCampaigns().stream()
                                .anyMatch(cc -> cc.getCandidate().getUser().getId().equals(authUserId));

        if (!hasAccess) {
            throw new ForbiddenException("Non hai i permessi necessari per visualizzare questa campagna");
        }

        return new CampaignSummaryDto(campaign);
    }

    @Override
    @Transactional
    public void addCandidateInCampaign(@NotNull CampaignAddCandidateDto dto, Long authUserId) {
        // 1. Recupero dati e validazione Org
        User authUser = userRepository.findById(authUserId)
                .orElseThrow(() -> new NotFoundException("Utente autenticato non trovato"));

        Campaign campaign = campaignRepository.findById(dto.campaignId())
                .orElseThrow(() -> new NotFoundException("Campagna non trovata"));

        if (authUser.getOrg() == null || !campaign.getList().getOrg().getId().equals(authUser.getOrg().getId())) {
            throw new ForbiddenException("La campagna non appartiene alla tua organizzazione");
        }

        // 2. Controllo Permessi
        boolean hasAccess = permissionService.hasPermission(authUserId, "manager_candidate_in_campaign_organization") ||
                permissionService.hasPermissionOnList(authUserId, campaign.getList().getId(), "manager_candidate_in_campaign_list");

        if (!hasAccess) throw new ForbiddenException("Non hai i permessi per aggiungere candidati a questa campagna");

        // 3. Recupero Candidato
        Candidate candidate = candidateRepository.findById(dto.candidateId())
                .orElseThrow(() -> new NotFoundException("Il candidato specificato non esiste"));

        // 4. Controllo duplicati (Uso l'ID del candidato direttamente per maggiore efficienza)
        boolean exists = campaign.getCandidateCampaigns().stream()
                .anyMatch(cc -> cc.getCandidate().getId().equals(dto.candidateId()));

        if (exists) throw new BadRequestException("Il candidato è già presente in questa campagna");

        // 5. Creazione Associazione
        CandidateCampaign association = new CandidateCampaign();
        association.setCampaign(campaign);
        association.setCandidate(candidate);

        // Importante: Aggiungiamo l'associazione alla campagna (helper method)
        campaign.addCandidate(association);

        // 6. Gestione Posizione (se presente)
        if (dto.positionInList() != null) {
            CandidatePositionCampaign cpc = new CandidatePositionCampaign();
            cpc.setCandidateCampaign(association);
            cpc.setPositionInList(dto.positionInList());
            cpc.setPosition(positionRepository.findById(dto.positonId())
                    .orElseThrow(() -> new NotFoundException("Posizione non trovata")
            ));
            // Se non hai CascadeType.ALL su CandidateCampaign -> CandidatePositionCampaign, salvalo esplicitamente
            candidatePositionCampaignRepository.save(cpc);
        }

        // 7. Salvataggio finale
        campaignRepository.save(campaign);
    }

    @Transactional
    public void removeCandidateFromCampaign(@NotNull Long candidateId, @NotNull Long campaignId, @NotNull Long authUserId) {
        // 1. Recupero Utente e Campagna (Recuperiamo il candidato solo se serve davvero)
        User authUser = userRepository.findById(authUserId)
                .orElseThrow(() -> new NotFoundException("Utente autenticato non trovato"));

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("Campagna non trovata"));

        // 2. Controllo Isolamento Organizzazione
        if (authUser.getOrg() == null || !campaign.getList().getOrg().getId().equals(authUser.getOrg().getId())) {
            throw new ForbiddenException("La campagna non appartiene alla tua organizzazione");
        }

        // 3. Controllo Permessi (Aggiunto campaign.getList().getId())
        boolean hasPermission =
                permissionService.hasPermission(authUserId, "manager_candidate_in_campaign_organization") ||
                        permissionService.hasPermissionOnList(authUserId, campaign.getList().getId(), "manager_candidate_in_campaign_list");

        if (!hasPermission) {
            throw new ForbiddenException("Non hai i permessi per rimuovere un candidato dalla campagna");
        }

        // 4. Ricerca dell'associazione specifica
        CandidateCampaign toRemove = campaign.getCandidateCampaigns()
                .stream()
                .filter(cc -> cc.getCandidate().getId().equals(candidateId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Il candidato specificato non è associato a questa campagna"));

        // 5. Gestione manuale delle posizioni (Se non hai il cascade attivo nelle Entity)
        Set<CandidatePositionCampaign> cpcs = candidatePositionCampaignRepository.findAllByCandidateCampaignId(toRemove.getId());
        if (!cpcs.isEmpty()) {
            candidatePositionCampaignRepository.deleteAll(cpcs);
        }

        // 6. Rimozione e salvataggio
        campaign.removeCandidate(toRemove);
        campaignRepository.save(campaign);
    }

    @Override
    @Transactional
    public void deleteCampaign(Long campaignId, Long authUserId) {
        // 1. Recupero e Validazione
        User authUser = userRepository.findById(authUserId)
                .orElseThrow(() -> new NotFoundException("Utente autenticato non trovato"));

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("Campagna non trovata"));

        // 2. Controllo Isolamento Org
        if (authUser.getOrg() == null || !campaign.getList().getOrg().getId().equals(authUser.getOrg().getId())) {
            throw new ForbiddenException("La campagna non appartiene alla tua organizzazione");
        }

        // 3. Controllo Permessi (Attenzione ai refusi nelle stringhe!)
        boolean hasPermission =
                permissionService.hasPermission(authUserId, "delete_campaign_organization") ||
                        permissionService.hasPermissionOnList(authUserId, campaign.getList().getId(), "delete_campaign_list");

        if (!hasPermission) {
            throw new ForbiddenException("Non hai i permessi necessari per eliminare questa campagna");
        }

        // 4. Pulizia a catena delle relazioni
        Set<CandidateCampaign> ccs = campaign.getCandidateCampaigns();

        // Recupero tutte le posizioni collegate ai candidati di questa campagna
        Set<CandidatePositionCampaign> cpcs = ccs.stream()
                .map(cc -> candidatePositionCampaignRepository.findAllByCandidateCampaignId(cc.getId()))
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        // 5. Eliminazione fisica (ordine: Figli -> Genitori)
        if (!cpcs.isEmpty()) {
            candidatePositionCampaignRepository.deleteAll(cpcs);
        }

        // Rimuoviamo le associazioni intermedie
        if (!ccs.isEmpty()) {
            // Se non hai orphanRemoval=true, devi cancellarli tramite repository o svuotare la collection e flushare
            candidateCampaignRepository.deleteAll(ccs);
            campaign.getCandidateCampaigns().clear();
        }

        // 6. Eliminazione Campagna
        campaignRepository.delete(campaign);
    }
}