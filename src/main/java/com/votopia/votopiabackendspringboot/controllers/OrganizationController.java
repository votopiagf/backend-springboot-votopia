package com.votopia.votopiabackendspringboot.controllers;

import com.votopia.votopiabackendspringboot.dtos.SuccessResponse;
import com.votopia.votopiabackendspringboot.dtos.organization.OrganizationSummaryDto;
import com.votopia.votopiabackendspringboot.services.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/organization")
@Tag(name = "Organization", description = "Endpoint per il recupero delle informazioni dell'organizzazione.")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;

    @Operation(
            summary = "Recupera organizzazione tramite codice",
            description = "Endpoint pubblico per ottenere i dettagli (nome, logo, colori) di un'organizzazione usando il suo codice univoco. " +
                    "Utile per caricare il branding nel frontend prima del login."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Organizzazione trovata con successo"),
            @ApiResponse(responseCode = "400", description = "Codice organizzazione mancante o non valido"),
            @ApiResponse(responseCode = "404", description = "Nessuna organizzazione trovata per il codice fornito")
    })
    @GetMapping("/by-code/")
    @SecurityRequirements // Rimuove esplicitamente l'obbligo del lucchetto (Security Scheme) in Swagger UI
    public ResponseEntity<SuccessResponse<OrganizationSummaryDto>> getByCode(
            @Parameter(description = "Il codice univoco dell'organizzazione (es. VOTOPIA_01)", required = true)
            @RequestParam(value = "organization_id") @Valid String code){

        OrganizationSummaryDto org = organizationService.getOrganizationByCode(code);

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        true,
                        HttpStatus.OK.value(),
                        org,
                        "Organizzazione trovata",
                        System.currentTimeMillis()
                )
        );
    }
}