package com.votopia.votopiabackendspringboot.controllers;

import com.votopia.votopiabackendspringboot.config.CustomUserDetails;
import com.votopia.votopiabackendspringboot.dtos.SuccessResponse;
import com.votopia.votopiabackendspringboot.dtos.role.RoleCreateDto;
import com.votopia.votopiabackendspringboot.dtos.role.RoleInfoResponse;
import com.votopia.votopiabackendspringboot.dtos.role.RoleSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.role.RoleUpdateDto;
import com.votopia.votopiabackendspringboot.services.auth.RoleService;
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
@RequestMapping("/api/roles")
@Tag(name = "Role Management", description = "Endpoint per la gestione dei ruoli e l'assegnazione dei permessi alle liste.")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @Operation(
            summary = "Crea un nuovo ruolo",
            description = "Crea un ruolo personalizzato associato all'organizzazione o a una lista specifica. " +
                    "I permessi inclusi nel DTO verranno collegati al nuovo ruolo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ruolo creato correttamente"),
            @ApiResponse(responseCode = "400", description = "Dati di input non validi o permessi inesistenti"),
            @ApiResponse(responseCode = "403", description = "Accesso negato all'organizzazione o lista target"),
            @ApiResponse(responseCode = "409", description = "Esiste già un ruolo con questo nome nel contesto specificato")
    })
    @PostMapping("/create/")
    public ResponseEntity<SuccessResponse<RoleSummaryDto>> create(
            @RequestBody @Valid RoleCreateDto roleCreate,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        RoleSummaryDto roleCreated = roleService.create(roleCreate, userDetails.getId());

        return new ResponseEntity<>(new SuccessResponse<>(
                true, 201, roleCreated, "Ruolo creato con successo", System.currentTimeMillis()
        ), HttpStatus.CREATED);
    }

    @Operation(
            summary = "Elimina un ruolo",
            description = "Rimuove permanentemente un ruolo. Questa operazione scollegherà tutti gli utenti associati a questo ruolo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ruolo eliminato con successo"),
            @ApiResponse(responseCode = "404", description = "Ruolo non trovato"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti per eliminare ruoli")
    })
    @DeleteMapping("/delete/")
    public ResponseEntity<SuccessResponse<Void>> delete(
            @Parameter(description = "ID del ruolo da eliminare", required = true)
            @RequestParam(value = "target_role_id") Long roleTargetId,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        roleService.delete(roleTargetId, userDetails.getId());

        return ResponseEntity.ok(new SuccessResponse<>(
                true, 200, null, "Ruolo eliminato con successo", System.currentTimeMillis()
        ));
    }

    @Operation(
            summary = "Recupera tutti i ruoli visibili",
            description = "Restituisce l'elenco dei ruoli dell'organizzazione. Se viene fornito target_list_id, restituisce solo i ruoli applicabili a quella lista."
    )
    @GetMapping("/all/")
    public ResponseEntity<SuccessResponse<Set<RoleSummaryDto>>> getAll(
            @Parameter(description = "Filtra i ruoli per una lista specifica.")
            @RequestParam(required = false, value = "target_list_id") Long targetListId,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Set<RoleSummaryDto> roles = roleService.getAllVisible(userDetails.getId(), targetListId);

        return ResponseEntity.ok(new SuccessResponse<>(
                true, 200, roles, "Ruoli ottenuti con successo", System.currentTimeMillis()
        ));
    }

    @Operation(
            summary = "Ottieni dettagli completi di un ruolo",
            description = "Restituisce le informazioni di un ruolo, inclusa la lista completa dei permessi associati."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dettagli ruolo recuperati"),
            @ApiResponse(responseCode = "404", description = "Ruolo non trovato o non appartenente alla tua Org")
    })
    @GetMapping("/info/")
    public ResponseEntity<SuccessResponse<RoleInfoResponse>> info(
            @Parameter(description = "ID del ruolo di cui visualizzare i dettagli", required = true)
            @RequestParam(value = "target_role_id") Long targetRoleId,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        RoleInfoResponse role = roleService.getRoleInformation(userDetails.getId(), targetRoleId);

        return ResponseEntity.ok(new SuccessResponse<>(
                true, 200, role, "Ruolo ottenuto con successo", System.currentTimeMillis()
        ));
    }

    @Operation(
            summary = "Aggiorna un ruolo",
            description = "Modifica il nome del ruolo o la sua lista di permessi associati."
    )
    @PutMapping("/update/")
    public ResponseEntity<SuccessResponse<RoleSummaryDto>> update(
            @RequestBody @Valid RoleUpdateDto role,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        RoleSummaryDto roleUpdated = roleService.update(role, userDetails.getId());

        return ResponseEntity.ok(new SuccessResponse<>(
                true, 200, roleUpdated, "Ruolo aggiornato con successo", System.currentTimeMillis()
        ));
    }
}