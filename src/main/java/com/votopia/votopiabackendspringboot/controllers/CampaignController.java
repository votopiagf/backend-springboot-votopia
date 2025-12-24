package com.votopia.votopiabackendspringboot.controllers;

import com.votopia.votopiabackendspringboot.config.CustomUserDetails;
import com.votopia.votopiabackendspringboot.dtos.SuccessResponse;
import com.votopia.votopiabackendspringboot.dtos.campaign.CampaignCreateDto;
import com.votopia.votopiabackendspringboot.dtos.campaign.CampaignSummaryDto;
import com.votopia.votopiabackendspringboot.services.CampaignService;
import io.micrometer.common.lang.Nullable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/campaigns")
@Tag(name = "Campaigns", description = "Endpoint per la gestione delle campagne elettorali e informative.")
public class CampaignController {

    @Autowired
    private CampaignService campaignService;

    @Operation(
            summary = "Crea una nuova campagna",
            description = "Crea una campagna legata a una specifica lista elettorale. " +
                    "Verifica automaticamente che la data di inizio sia precedente alla data di fine " +
                    "e che l'utente abbia i permessi necessari (Org-level o List-level)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Campagna creata con successo"),
            @ApiResponse(responseCode = "400", description = "Dati non validi (es. date errate o campi mancanti)"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti per creare campagne in questa lista"),
            @ApiResponse(responseCode = "404", description = "Lista di destinazione non trovata nell'organizzazione")
    })
    @PostMapping("/create/")
    public ResponseEntity<SuccessResponse<CampaignSummaryDto>> create(
            @RequestBody CampaignCreateDto campaign,
            Authentication authentication) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        CampaignSummaryDto campaignSummaryDto = this.campaignService.create(campaign, userId);

        return new ResponseEntity<>(
                new SuccessResponse<>(
                        true,
                        HttpStatus.CREATED.value(),
                        campaignSummaryDto,
                        "Campaign creata con successo",
                        System.currentTimeMillis()
                ), HttpStatus.CREATED
        );
    }

    @Operation(
            summary = "Recupera tutte le campagne visibili",
            description = "Restituisce l'elenco delle campagne basato sui permessi dell'utente. " +
                    "Se 'target_list_id' è fornito, filtra per quella lista (richiede permessi sulla lista o sull'organizzazione). " +
                    "Se omesso, restituisce tutte le campagne dell'organizzazione (richiede permessi a livello Org)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Elenco campagne recuperato con successo"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti per visualizzare le campagne richieste"),
            @ApiResponse(responseCode = "404", description = "Lista target non trovata o non appartenente alla tua organizzazione")
    })
    @GetMapping("/all/")
    public ResponseEntity<SuccessResponse<Set<CampaignSummaryDto>>> getAll(@RequestParam(value = "target_list_id", required = false) Long targetListId, Authentication authentication){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        Set<CampaignSummaryDto> campaigns = campaignService.getAll(targetListId, userId);
        return ResponseEntity.ok(
                new SuccessResponse<>(
                        true,
                        HttpStatus.OK.value(),
                        campaigns,
                        "Campagne ottenute con successo",
                        System.currentTimeMillis()
                )
        );
    }

    @GetMapping("/info/")
    public ResponseEntity<SuccessResponse<CampaignSummaryDto>> get(@RequestParam(value = "target_campaign_id") Long targetCampaignId, Authentication authentication){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        CampaignSummaryDto camp = campaignService.get(targetCampaignId, userId);
        return ResponseEntity.ok(
                new SuccessResponse<>(
                        true,
                        HttpStatus.OK.value(),
                        camp,
                        "Campagna ottenuta con successo",
                        System.currentTimeMillis()
                )
        );
    }
}