package com.votopia.votopiabackendspringboot.controllers;

import com.votopia.votopiabackendspringboot.config.CustomUserDetails;
import com.votopia.votopiabackendspringboot.dtos.SuccessResponse;
import com.votopia.votopiabackendspringboot.dtos.campaign.CampaignAddCandidateDto;
import com.votopia.votopiabackendspringboot.dtos.campaign.CampaignCreateDto;
import com.votopia.votopiabackendspringboot.dtos.campaign.CampaignSummaryDto;
import com.votopia.votopiabackendspringboot.services.CampaignService;
import io.micrometer.common.lang.Nullable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Operation(
            summary = "Ottieni dettagli campagna",
            description = "Recupera le informazioni dettagliate di una singola campagna. " +
                    "L'accesso è consentito se l'utente è Admin dell'Organizzazione, " +
                    "se ha permessi sulla Lista, o se è un Candidato della campagna stessa."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dettagli campagna recuperati con successo"),
            @ApiResponse(responseCode = "403", description = "Accesso negato: non hai i permessi o la campagna appartiene a un'altra Org"),
            @ApiResponse(responseCode = "404", description = "Campagna non trovata")
    })
    @GetMapping("/info/")
    public ResponseEntity<SuccessResponse<CampaignSummaryDto>> get(
            @Parameter(description = "ID della campagna da visualizzare", required = true)
            @RequestParam(value = "target_campaign_id") Long targetCampaignId,
            Authentication authentication) {

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

    @Operation(
            summary = "Aggiungi candidato alla campagna",
            description = "Associa un candidato esistente a una campagna. " +
                    "È possibile specificare opzionalmente la posizione in lista. " +
                    "Richiede permessi di gestione a livello Org o Lista."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Candidato associato con successo"),
            @ApiResponse(responseCode = "400", description = "Il candidato è già presente nella campagna"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti per aggiungere candidati"),
            @ApiResponse(responseCode = "404", description = "Campagna o Candidato non trovati")
    })
    @PostMapping("/add-candidate/")
    public ResponseEntity<SuccessResponse<Void>> addCandidate(
            @RequestBody @Valid CampaignAddCandidateDto dto,
            Authentication authentication) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        campaignService.addCandidateInCampaign(dto, userId);
        return ResponseEntity.ok(
                new SuccessResponse<>(
                        true,
                        HttpStatus.OK.value(),
                        null,
                        "Candidato aggiunto con successo",
                        System.currentTimeMillis()
                )
        );
    }

    @Operation(
            summary = "Rimuovi candidato",
            description = "Elimina l'associazione tra un candidato e una campagna. " +
                    "Richiede privilegi di gestione sulla lista o sull'organizzazione. " +
                    "L'operazione è irreversibile e rimuove anche la posizione occupata in lista."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Candidato rimosso con successo"),
            @ApiResponse(responseCode = "403", description = "Accesso negato: permessi insufficienti o violazione isolamento Org"),
            @ApiResponse(responseCode = "404", description = "Associazione non trovata: il candidato non è presente in questa campagna")
    })
    @DeleteMapping("/delete-candidate/")
    public ResponseEntity<SuccessResponse<Void>> deleteCandidate(
            @Parameter(description = "ID del candidato da escludere", required = true)
            @RequestParam(value = "candidate_id") Long candidateId,

            @Parameter(description = "ID della campagna di riferimento", required = true)
            @RequestParam(value = "campaign_id") Long campaignId,
            Authentication authentication
    ){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        campaignService.removeCandidateFromCampaign(candidateId, campaignId, userId);
        return ResponseEntity.ok(
                new SuccessResponse<>(
                        true,
                        HttpStatus.OK.value(),
                        null,
                        "Candidato rimosso con successo",
                        System.currentTimeMillis()
                )
        );
    }

    @Operation(
            summary = "Elimina una campagna",
            description = "Rimuove permanentemente una campagna dal database. " +
                    "L'operazione comporta la cancellazione automatica di tutti i candidati associati " +
                    "e delle loro posizioni in lista. Questa azione non è reversibile."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Campagna e dati correlati eliminati con successo"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Accesso negato: permessi insufficienti o tentativo di accesso cross-org"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Campagna non trovata"
            )
    })
    @DeleteMapping("/delete/")
    public ResponseEntity<SuccessResponse<Void>> deleteCampaign(
            @Parameter(description = "ID della campagna da eliminare", required = true)
            @RequestParam(value = "campaing_id") Long campaignId,
            Authentication authentication) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        campaignService.deleteCampaign(campaignId, userDetails.getId());

        return ResponseEntity.ok(new SuccessResponse<>(true, 200, null, "Campagna eliminata con successo", System.currentTimeMillis()));
    }
}