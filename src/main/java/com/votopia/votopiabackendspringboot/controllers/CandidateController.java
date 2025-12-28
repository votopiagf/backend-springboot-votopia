package com.votopia.votopiabackendspringboot.controllers;

import com.votopia.votopiabackendspringboot.config.CustomUserDetails;
import com.votopia.votopiabackendspringboot.dtos.SuccessResponse;
import com.votopia.votopiabackendspringboot.dtos.candidate.CandidateCreateDto;
import com.votopia.votopiabackendspringboot.dtos.candidate.CandidateSummaryDto;
import com.votopia.votopiabackendspringboot.services.CandidateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
@RequestMapping("/api/candidates")
@Tag(name = "Candidates", description = "Endpoint per la gestione dei candidati")
public class CandidateController {
    @Autowired private CandidateService candidateService;

    @Operation(
            summary = "Crea un nuovo candidato",
            description = "Crea un'associazione tra un utente e una lista come candidato. " +
                    "Verifica i permessi dell'operatore, la candidabilità dell'utente target " +
                    "e l'appartenenza della lista all'organizzazione.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Candidato creato con successo"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Dati non validi o candidato già esistente nella lista"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Permessi insufficienti o utente target non candidabile"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Lista, Utente o File non trovati"
                    )
            }
    )
    @PostMapping("/create/")
    public ResponseEntity<SuccessResponse<CandidateSummaryDto>> create(
            @RequestBody @Valid CandidateCreateDto dto,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        return new ResponseEntity<>(
                new SuccessResponse<>(
                        true,
                        HttpStatus.CREATED.value(),
                        candidateService.create(dto, userId),
                        "Candidato creato con successo",
                        System.currentTimeMillis()
                ), HttpStatus.CREATED
        );
    }

    @Operation(
            summary = "Recupera i candidati di una lista",
            description = "Restituisce l'elenco di tutti i candidati appartenenti a una lista specifica, " +
                    "previa verifica dell'appartenenza della lista all'organizzazione dell'utente.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Elenco recuperato con successo"),
                    @ApiResponse(responseCode = "403", description = "Permessi insufficienti per visualizzare i candidati"),
                    @ApiResponse(responseCode = "404", description = "Lista non trovata nell'organizzazione")
            }
    )
    @GetMapping("/all-by-list/")
    public ResponseEntity<SuccessResponse<Set<CandidateSummaryDto>>> getAllByList(
            @Parameter(description = "ID della lista da visualizzare tutti i candidati", required = true)
            @RequestParam(value = "target_list_id") @Valid Long listId,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        return new ResponseEntity<>(
                new SuccessResponse<>(
                        true,
                        HttpStatus.OK.value(),
                        candidateService.getAllByList(listId, userId),
                        "Candidato creato con successo",
                        System.currentTimeMillis()
                ), HttpStatus.CREATED
        );
    }
}
