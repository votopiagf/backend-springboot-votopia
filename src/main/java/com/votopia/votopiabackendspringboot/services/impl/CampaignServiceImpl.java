package com.votopia.votopiabackendspringboot.services.impl;

import com.votopia.votopiabackendspringboot.dtos.campaign.CampaignAddCandidateDto;
import com.votopia.votopiabackendspringboot.dtos.campaign.CampaignCreateDto;
import com.votopia.votopiabackendspringboot.dtos.campaign.CampaignSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.campaign.CampaignUpdateDto;
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
import com.votopia.votopiabackendspringboot.repositories.campaigns.*;
import com.votopia.votopiabackendspringboot.repositories.lists.ListRepository;
import com.votopia.votopiabackendspringboot.services.CampaignService;
import com.votopia.votopiabackendspringboot.services.auth.AuthService;
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

/**
 * Implementazione del servizio CampaignService.
 * La logica di recupero dell'utente autenticato è delegata al PermissionService.
 */
@Service
@Slf4j
public class CampaignServiceImpl implements CampaignService {

    @Autowired private CampaignRepository campaignRepository;
    @Autowired private ListRepository listRepository;
    @Autowired private PermissionService permissionService; // Gestisce ora il recupero dell'utente
    @Autowired private CandidateRepository candidateRepository;
    @Autowired private CandidatePositionCampaignRepository candidatePositionCampaignRepository;
    @Autowired private PositionRepository positionRepository;
    @Autowired private CandidateCampaignRepository candidateCampaignRepository;
    @Autowired private AuthService authService;

    @Override
    @Transactional
    public CampaignSummaryDto create(CampaignCreateDto dto, Long authUserId) {
        // Recupero delegato al service di auth
        User authUser = authService.getAuthenticatedUser(authUserId);

        List listTarget = listRepository
                .findByIdAndOrgId(dto.listId(), authUser.getOrg().getId())
                .orElseThrow(() -> new NotFoundException("Lista target non trovata nella tua Organizzazione"));

        if (dto.startDate().isAfter(dto.endDate()) || dto.startDate().isEqual(dto.endDate())) {
            throw new BadRequestException("La data di inizio deve essere precedente alla data di fine.");
        }

        validatePermission(authUserId, dto.listId(),
                "create_campaign_organization", "create_campaign_list",
                "Non hai il permesso sufficiente per creare campagne in questa Lista.");

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
        User authUser = authService.getAuthenticatedUser(authUserId);
        Long orgId = authUser.getOrg().getId();

        boolean canOrg = permissionService.hasPermission(authUserId, "view_all_campaign_organization");

        if (listId == null) {
            if (!canOrg) throw new ForbiddenException("Devi specificare una lista o avere permessi globali.");
            return campaignRepository.findAllByListOrgId(orgId).stream()
                    .map(CampaignSummaryDto::new).collect(Collectors.toSet());
        }

        listRepository.findByIdAndOrgId(listId, orgId)
                .orElseThrow(() -> new NotFoundException("Lista non trovata nella tua Organizzazione"));

        if (canOrg || permissionService.hasPermissionOnList(authUserId, listId, "view_all_campaign_list")) {
            return campaignRepository.findAllByListId(listId).stream()
                    .map(CampaignSummaryDto::new).collect(Collectors.toSet());
        }

        throw new ForbiddenException("Non hai i permessi per vedere le campagne di questa lista.");
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignSummaryDto get(Long campaignId, Long authUserId) {
        User authUser = authService.getAuthenticatedUser(authUserId);
        Campaign campaign = getCampaignAndValidateOrg(campaignId, authUser);

        boolean hasAccess = permissionService.hasPermission(authUserId, "view_all_campaign_organization") ||
                permissionService.hasPermissionOnList(authUserId, campaign.getList().getId(), "view_all_campaign_list") ||
                campaign.getCandidateCampaigns().stream().anyMatch(cc -> cc.getCandidate().getUser().getId().equals(authUserId));

        if (!hasAccess) throw new ForbiddenException("Non hai i permessi necessari per visualizzare questa campagna");

        return new CampaignSummaryDto(campaign);
    }

    @Override
    @Transactional
    public void addCandidateInCampaign(@NotNull CampaignAddCandidateDto dto, Long authUserId) {
        User authUser = authService.getAuthenticatedUser(authUserId);
        Campaign campaign = getCampaignAndValidateOrg(dto.campaignId(), authUser);

        validatePermission(authUserId, campaign.getList().getId(),
                "manager_candidate_in_campaign_organization", "manager_candidate_in_campaign_list",
                "Non hai i permessi per aggiungere candidati a questa campagna");

        Candidate candidate = candidateRepository.findById(dto.candidateId())
                .orElseThrow(() -> new NotFoundException("Il candidato specificato non esiste"));

        boolean exists = campaign.getCandidateCampaigns().stream()
                .anyMatch(cc -> cc.getCandidate().getId().equals(dto.candidateId()));
        if (exists) throw new BadRequestException("Il candidato è già presente in questa campagna");

        CandidateCampaign association = new CandidateCampaign();
        association.setCampaign(campaign);
        association.setCandidate(candidate);
        campaign.addCandidate(association);

        if (dto.positionInList() != null) {
            CandidatePositionCampaign cpc = new CandidatePositionCampaign();
            cpc.setCandidateCampaign(association);
            cpc.setPositionInList(dto.positionInList());
            cpc.setPosition(positionRepository.findById(dto.positonId())
                    .orElseThrow(() -> new NotFoundException("Posizione non trovata")));
            candidatePositionCampaignRepository.save(cpc);
        }

        campaignRepository.save(campaign);
    }

    @Override
    @Transactional
    public void removeCandidateFromCampaign(@NotNull Long candidateId, @NotNull Long campaignId, @NotNull Long authUserId) {
        User authUser = authService.getAuthenticatedUser(authUserId);
        Campaign campaign = getCampaignAndValidateOrg(campaignId, authUser);

        validatePermission(authUserId, campaign.getList().getId(),
                "manager_candidate_in_campaign_organization", "manager_candidate_in_campaign_list",
                "Non hai i permessi per rimuovere un candidato");

        CandidateCampaign toRemove = campaign.getCandidateCampaigns().stream()
                .filter(cc -> cc.getCandidate().getId().equals(candidateId))
                .findFirst().orElseThrow(() -> new NotFoundException("Candidato non associato a questa campagna"));

        Set<CandidatePositionCampaign> cpcs = candidatePositionCampaignRepository.findAllByCandidateCampaignId(toRemove.getId());
        if (!cpcs.isEmpty()) candidatePositionCampaignRepository.deleteAll(cpcs);

        campaign.removeCandidate(toRemove);
        campaignRepository.save(campaign);
    }

    @Override
    @Transactional
    public void deleteCampaign(Long campaignId, Long authUserId) {
        User authUser = authService.getAuthenticatedUser(authUserId);
        Campaign campaign = getCampaignAndValidateOrg(campaignId, authUser);

        validatePermission(authUserId, campaign.getList().getId(),
                "delete_campaign_organization", "delete_campaign_list",
                "Non hai i permessi necessari per eliminare questa campagna");

        Set<CandidateCampaign> ccs = campaign.getCandidateCampaigns();
        Set<CandidatePositionCampaign> cpcs = ccs.stream()
                .map(cc -> candidatePositionCampaignRepository.findAllByCandidateCampaignId(cc.getId()))
                .flatMap(Set::stream).collect(Collectors.toSet());

        if (!cpcs.isEmpty()) candidatePositionCampaignRepository.deleteAll(cpcs);
        if (!ccs.isEmpty()) {
            candidateCampaignRepository.deleteAll(ccs);
            campaign.getCandidateCampaigns().clear();
        }

        campaignRepository.delete(campaign);
    }

    @Override
    @Transactional
    public CampaignSummaryDto update(CampaignUpdateDto dto, Long authUserId){
        User authUser = authService.getAuthenticatedUser(authUserId);

        Campaign campaign = getCampaignAndValidateOrg(dto.id(), authUser);

        validatePermission(authUserId, campaign.getList().getId(),
                "update_campaign_organization", "update_campaign_list",
                "Non hai i permessi necessari per modificare questa campagna");

        if (dto.name() != null) campaign.setName(dto.name());
        if (dto.description() != null) campaign.setDescription(dto.description());
        if (dto.endDate() != null) campaign.setEndDate(dto.endDate());
        if (dto.startDate() != null) campaign.setStartDate(dto.startDate());

        return new CampaignSummaryDto(campaignRepository.save(campaign));
    }

    // --- METODI PRIVATI DI UTILITY (HELPERS) ---

    private Campaign getCampaignAndValidateOrg(Long campaignId, User authUser) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("Campagna non trovata"));
        if (!campaign.getList().getOrg().getId().equals(authUser.getOrg().getId())) {
            throw new ForbiddenException("Accesso negato: la campagna appartiene a un'altra organizzazione");
        }
        return campaign;
    }

    private void validatePermission(Long authUserId, Long listId, String orgPerm, String listPerm, String errorMsg) {
        boolean hasAccess = permissionService.hasPermission(authUserId, orgPerm) ||
                permissionService.hasPermissionOnList(authUserId, listId, listPerm);
        if (!hasAccess) throw new ForbiddenException(errorMsg);
    }
}