package com.votopia.votopiabackendspringboot.controllers;

import com.votopia.votopiabackendspringboot.dtos.SuccessResponse;
import com.votopia.votopiabackendspringboot.dtos.auth.LoginRequestDto;
import com.votopia.votopiabackendspringboot.dtos.auth.LoginSummaryDto;
import com.votopia.votopiabackendspringboot.services.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth/")
@Tag(name = "Authentication", description = "Endpoint per l'accesso al sistema e la gestione dei token JWT.")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(
            summary = "Login Utente",
            description = "Autentica l'utente tramite email e password. " +
                    "In caso di successo, restituisce il token JWT e i dati dell'utente con l'organizzazione di appartenenza."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticazione riuscita, token generato"),
            @ApiResponse(responseCode = "401", description = "Credenziali non valide (email o password errate)"),
            @ApiResponse(responseCode = "400", description = "Dati di login mancanti o malformati")
    })
    @PostMapping("login/")
    @SecurityRequirements // Rimuove l'obbligo del lucchetto JWT per questo endpoint in Swagger UI
    public ResponseEntity<SuccessResponse<LoginSummaryDto>> login(@RequestBody @Valid LoginRequestDto request){
        log.info("Tentativo di login per l'utente: {}", request.getEmail());
        LoginSummaryDto response = authService.login(request);
        return ResponseEntity.ok(
                new SuccessResponse<>(
                        true,
                        HttpStatus.OK.value(),
                        response,
                        "Login effettuato con successo",
                        System.currentTimeMillis()
                )
        );
    }
}