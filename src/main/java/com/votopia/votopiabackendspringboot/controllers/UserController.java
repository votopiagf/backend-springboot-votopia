package com.votopia.votopiabackendspringboot.controllers;

import com.votopia.votopiabackendspringboot.config.CustomUserDetails;
import com.votopia.votopiabackendspringboot.dtos.SuccessResponse;
import com.votopia.votopiabackendspringboot.dtos.user.UserCreateDto;
import com.votopia.votopiabackendspringboot.dtos.user.UserSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.user.UserUpdateDto;
import com.votopia.votopiabackendspringboot.services.auth.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Slf4j
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "Endpoint per la gestione degli utenti, registrazione e permessi.")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(
            summary = "Registra un nuovo utente",
            description = "Crea un nuovo utente all'interno dell'organizzazione dell'utente autenticato. Richiede permessi di amministrazione."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Utente creato con successo"),
            @ApiResponse(responseCode = "400", description = "Dati di input non validi"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti"),
            @ApiResponse(responseCode = "409", description = "Email o Username già esistenti")
    })
    @PostMapping("/register/")
    public ResponseEntity<SuccessResponse<UserSummaryDto>> register(
            @RequestBody @Valid UserCreateDto user,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UserSummaryDto userRegister = userService.register(user, userDetails.getId());

        return new ResponseEntity<>(new SuccessResponse<>(
                true, HttpStatus.CREATED.value(), userRegister, "Utente registrato con successo", System.currentTimeMillis()
        ), HttpStatus.CREATED);
    }

    @Operation(
            summary = "Ottieni informazioni utente",
            description = "Recupera i dettagli di un utente specifico o dell'utente autenticato (se target_user_id è omesso)."
    )
    @GetMapping("/info/")
    public ResponseEntity<SuccessResponse<UserSummaryDto>> info(
            @Parameter(description = "ID dell'utente da visualizzare. Se vuoto, restituisce l'utente corrente.")
            @RequestParam(value = "target_user_id", required = false) Long targetUserId,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UserSummaryDto userTarget = userService.getUserInformation(userDetails.getId(), targetUserId);

        return ResponseEntity.ok(new SuccessResponse<>(
                true, 200, userTarget, "Utente ottenuto con successo", System.currentTimeMillis()
        ));
    }

    @Operation(
            summary = "Lista tutti gli utenti visibili",
            description = "Restituisce l'elenco degli utenti appartenenti all'organizzazione. Può essere filtrato per una lista specifica."
    )
    @GetMapping("/all/")
    public ResponseEntity<SuccessResponse<Set<UserSummaryDto>>> all(
            @Parameter(description = "Filtra gli utenti associati a una determinata Lista.")
            @RequestParam(value = "target_list_id", required = false) Long targetListId,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Set<UserSummaryDto> usersList = userService.getAllVisibleUsers(userDetails.getId(), targetListId);

        return ResponseEntity.ok(new SuccessResponse<>(
                true, 200, usersList, "Utenti trovati con successo", System.currentTimeMillis()
        ));
    }

    @Operation(
            summary = "Elimina un utente",
            description = "Esegue la cancellazione di un utente. Richiede permessi di alto livello o che l'utente stia cancellando se stesso."
    )
    @DeleteMapping("/delete/")
    public ResponseEntity<SuccessResponse<Void>> delete(
            @Parameter(description = "ID dell'utente da eliminare.", required = true)
            @RequestParam(value = "target_user_id") Long targetUserId,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        userService.delete(targetUserId, userDetails.getId());

        return ResponseEntity.ok(new SuccessResponse<>(
                true, 200, null, "Utente eliminato con successo", System.currentTimeMillis()
        ));
    }

    @Operation(
            summary = "Aggiorna profilo utente",
            description = "Modifica i dati di un utente esistente (nome, email, password, ruoli)."
    )
    @PutMapping("/update/")
    public ResponseEntity<SuccessResponse<UserSummaryDto>> update(
            @RequestBody @Valid UserUpdateDto user,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UserSummaryDto userUpdated = userService.update(userDetails.getId(), user);

        return ResponseEntity.ok(new SuccessResponse<>(
                true, 200, userUpdated, "Utente modificato con successo", System.currentTimeMillis()
        ));
    }
}