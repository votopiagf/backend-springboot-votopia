package com.votopia.votopiabackendspringboot.controllers;

import com.votopia.votopiabackendspringboot.config.CustomUserDetails;
import com.votopia.votopiabackendspringboot.dtos.SuccessResponse;
import com.votopia.votopiabackendspringboot.dtos.list.ListCreateDto;
import com.votopia.votopiabackendspringboot.dtos.list.ListSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.list.ListUpdateDto;
import com.votopia.votopiabackendspringboot.services.ListService;
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

@RestController
@RequestMapping("/api/lists")
@Slf4j
@Tag(name = "Lists", description = "Endpoint per la gestione delle liste elettorali e dei censimenti.")
public class ListController {

    @Autowired
    private ListService listService;

    @Operation(
            summary = "Crea una nuova lista",
            description = "Aggiunge una lista all'organizzazione dell'utente autenticato. " +
                    "Richiede permessi amministrativi a livello di Organizzazione."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lista creata con successo"),
            @ApiResponse(responseCode = "400", description = "Dati di input non validi (es. nome mancante o duplicato)"),
            @ApiResponse(responseCode = "403", description = "Permesso negato: l'utente non può creare liste")
    })
    @PostMapping("/create/")
    public ResponseEntity<SuccessResponse<ListSummaryDto>> create(
            @RequestBody @Valid ListCreateDto list,
            Authentication authentication) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        ListSummaryDto listCreated = listService.create(list, userDetails.getId());

        return new ResponseEntity<>(
                new SuccessResponse<>(
                        true,
                        HttpStatus.CREATED.value(),
                        listCreated,
                        "Lista creata con successo",
                        System.currentTimeMillis()
                ), HttpStatus.CREATED
        );
    }

    @Operation(
            summary = "Aggiorna una lista esistente",
            description = "Modifica i dettagli di una lista. L'utente deve avere permessi di gestione sulla lista specifica o sull'organizzazione."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista aggiornata con successo"),
            @ApiResponse(responseCode = "404", description = "Lista non trovata"),
            @ApiResponse(responseCode = "403", description = "Permesso negato per la modifica")
    })
    @PutMapping("/update/")
    public ResponseEntity<SuccessResponse<ListSummaryDto>> update(
            @RequestBody @Valid ListUpdateDto list,
            Authentication authentication) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        ListSummaryDto listUpdated = listService.update(list, userDetails.getId());

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        true,
                        HttpStatus.OK.value(),
                        listUpdated,
                        "Lista modificata con successo",
                        System.currentTimeMillis()
                )
        );
    }

    @Operation(
            summary = "Ottieni tutte le liste visibili",
            description = "Restituisce l'elenco di tutte le liste che l'utente è autorizzato a vedere (basato sui ruoli assegnati)."
    )
    @ApiResponse(responseCode = "200", description = "Elenco delle liste recuperato correttamente")
    @GetMapping("/all/")
    public ResponseEntity<SuccessResponse<Set<ListSummaryDto>>> getAllVisibile(Authentication authentication) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Set<ListSummaryDto> lists = listService.getAllVisibleLists(userDetails.getId());

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        true,
                        HttpStatus.OK.value(),
                        lists,
                        "Tutte le liste sono state ottenute",
                        System.currentTimeMillis()
                )
        );
    }
}