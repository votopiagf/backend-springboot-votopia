package com.votopia.votopiabackendspringboot.controllers;

import com.votopia.votopiabackendspringboot.config.CustomUserDetails;
import com.votopia.votopiabackendspringboot.dtos.SuccessResponse;
import com.votopia.votopiabackendspringboot.dtos.list.ListOptionDto;
import com.votopia.votopiabackendspringboot.dtos.role.RoleOptionDto;
import com.votopia.votopiabackendspringboot.dtos.user.*;
import com.votopia.votopiabackendspringboot.services.auth.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.Set;

@RestController
@Slf4j
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "Endpoint per la gestione degli utenti, registrazione e permessi.")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(
            summary = "Inizializza la schermata di creazione utente",
            description = "Restituisce tutti i dati necessari per inizializzare il form di creazione utente nel frontend: liste disponibili e ruoli disponibili."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dati di inizializzazione ottenuti con successo"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti"),
            @ApiResponse(responseCode = "404", description = "Utente non trovato")
    })
    @GetMapping("/init-creation/")
    public ResponseEntity<SuccessResponse<UserCreationInitDto>> initializeUserCreation(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UserCreationInitDto initData = userService.getInitializationDataForUserCreation(userDetails.getId());

        return ResponseEntity.ok(new SuccessResponse<>(
                true, 200, initData, "Dati di inizializzazione ottenuti con successo", System.currentTimeMillis()
        ));
    }

    @Operation(
            summary = "Inizializza la schermata Users completa",
            description = "Restituisce TUTTI i dati necessari per inizializzare la schermata Users: liste, ruoli (org e list), statistiche (totale utenti, ruoli, liste) e scope di filtro disponibile."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dati schermata Users ottenuti con successo"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti"),
            @ApiResponse(responseCode = "404", description = "Utente non trovato")
    })
    @GetMapping("/init-screen/")
    public ResponseEntity<SuccessResponse<UsersScreenInitDto>> initializeUsersScreen(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UsersScreenInitDto screenData = userService.getUsersScreenInitialization(userDetails.getId());

        return ResponseEntity.ok(new SuccessResponse<>(
                true, 200, screenData, "Dati schermata Users ottenuti con successo", System.currentTimeMillis()
        ));
    }

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
    public ResponseEntity<SuccessResponse<UserDetailDto>> info(
            @Parameter(description = "ID dell'utente da visualizzare. Se vuoto, restituisce l'utente corrente.")
            @RequestParam(value = "target_user_id", required = false) Long targetUserId,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UserDetailDto userTarget = userService.getUserInformation(userDetails.getId(), targetUserId);

        return ResponseEntity.ok(new SuccessResponse<>(
                true, 200, userTarget, "Utente ottenuto con successo", System.currentTimeMillis()
        ));
    }

    @Operation(
            summary = "Lista tutti gli utenti visibili",
            description = "Restituisce l'elenco degli utenti appartenenti all'organizzazione. Può essere filtrato per una lista specifica."
    )
    @GetMapping("/all/")
    public ResponseEntity<SuccessResponse<Set<UserDetailDto>>> all(
            @Parameter(description = "Filtra gli utenti associati a una determinata Lista.")
            @RequestParam(value = "target_list_id", required = false) Long targetListId,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Set<UserDetailDto> usersList = userService.getAllVisibleUsers(userDetails.getId(), targetListId);

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
            summary = "Elimina una lista di utenti",
            description = "Esegue la cancellazione di una lista di utenti. Richiede permessi di alto livello."
    )
    @DeleteMapping("/delete/list/")
    public ResponseEntity<SuccessResponse<Void>> deleteList(
            @Parameter(description = "IDs degli utenti da eliminare.", required = true)
            @RequestParam(value = "ids_target_user") Set<Long> targetUsersIds,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        userService.deleteList(targetUsersIds, userDetails.getId());
        return ResponseEntity.ok(new SuccessResponse<>(
                true, 200, null, "Utenti eliminati con successo", System.currentTimeMillis()
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

    @Operation(
            summary = "Modifica una lista di utenti",
            description = "Modifica degli utenti all'interno dell'organizzazione dell'utente autenticato. Richiede permessi di amministrazione."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Utenti modificati con successo"),
            @ApiResponse(responseCode = "400", description = "Dati di input non validi"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti"),
            @ApiResponse(responseCode = "409", description = "Errore di conflitti con l'email")
    })
    @PutMapping("/update/list/")
    public ResponseEntity<SuccessResponse<Set<UserSummaryDto>>> updateListUsers(@RequestBody @Valid Set<UserUpdateDto> users, Authentication authentication){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        Set<UserSummaryDto> usersUpdated = userService.updateListUsers(users, userDetails.getId());
        return ResponseEntity.ok(
                new SuccessResponse<>(
                        true,
                        HttpStatus.OK.value(),
                        usersUpdated,
                        "Utenti modificati con successo",
                        System.currentTimeMillis()
                )
        );
    }

    @Operation(
            summary = "Registra una lista di utenti",
            description = "Crea dei nuovi utenti all'interno dell'organizzazione dell'utente autenticato. Richiede permessi di amministrazione."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Utenti creati con successo"),
            @ApiResponse(responseCode = "400", description = "Dati di input non validi"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti"),
            @ApiResponse(responseCode = "409", description = "Email o Username già esistenti")
    })
    @PostMapping("/register/list/")
    public ResponseEntity<SuccessResponse<Set<UserSummaryDto>>> registerListUsers(@RequestBody @Valid Set<UserCreateDto> users, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Set<UserSummaryDto> usersRegister = userService.registerListUsers(users, userDetails.getId());
        return new ResponseEntity<>(new SuccessResponse<>(
                true, HttpStatus.CREATED.value(), usersRegister, "Utenti registrati con successo", System.currentTimeMillis()
        ), HttpStatus.CREATED);
    }

    @Operation(
            summary = "Crea un file excel con una lista di utenti",
            description = "Crea un file di una lista di utenti all'interno dell'organizzazione dell'utente autenticato. Richiede permessi di amministrazione."
    )
    @GetMapping("/all/excel")
    public ResponseEntity<InputStreamResource> getExcelAllUsers(
            @Parameter(description = "Filtra gli utenti associati a una determinata Lista.")
            @RequestParam(value = "target_list_id", required = false) Long targetListId,
            Authentication authentication) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        ByteArrayInputStream in = userService.createExcelAllVisibleUsers(userDetails.getId(), targetListId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=users.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }

    @Operation(
            summary = "Ottieni liste assegnabili per creazione utente",
            description = "Restituisce le liste che l'utente autenticato può assegnare durante la creazione di un nuovo utente, rispettando i suoi permessi."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste restituite con successo"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti"),
            @ApiResponse(responseCode = "404", description = "Utente non trovato")
    })
    @GetMapping("/options/lists")
    public ResponseEntity<SuccessResponse<Set<ListOptionDto>>> getAssignableLists(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Set<ListOptionDto> lists = userService.getAssignableListsForUserCreation(userDetails.getId());

        return ResponseEntity.ok(new SuccessResponse<>(
                true, 200, lists, "Liste disponibili ottenute con successo", System.currentTimeMillis()
        ));
    }

    @Operation(
            summary = "Ottieni ruoli assegnabili per creazione utente",
            description = "Restituisce i ruoli che l'utente autenticato può assegnare durante la creazione di un nuovo utente, rispettando i suoi permessi gerarchici e il contesto (organizzazione o lista specifica)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ruoli restituiti con successo"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti"),
            @ApiResponse(responseCode = "404", description = "Utente o lista non trovati")
    })
    @GetMapping("/options/roles")
    public ResponseEntity<SuccessResponse<Set<RoleOptionDto>>> getAssignableRoles(
            @Parameter(description = "ID opzionale della lista target. Se omesso, restituisce ruoli a livello organizzazione.")
            @RequestParam(value = "target_list_id", required = false) Long targetListId,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Set<RoleOptionDto> roles = userService.getAssignableRolesForUserCreation(userDetails.getId(), targetListId);

        return ResponseEntity.ok(new SuccessResponse<>(
                true, 200, roles, "Ruoli disponibili ottenuti con successo", System.currentTimeMillis()
        ));
    }
}