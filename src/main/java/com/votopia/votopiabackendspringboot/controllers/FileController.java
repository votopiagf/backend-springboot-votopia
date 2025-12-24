package com.votopia.votopiabackendspringboot.controllers;

import com.votopia.votopiabackendspringboot.config.CustomUserDetails;
import com.votopia.votopiabackendspringboot.dtos.SuccessResponse;
import com.votopia.votopiabackendspringboot.dtos.file.FileSummaryDto;
import com.votopia.votopiabackendspringboot.services.files.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@Slf4j
@Tag(name = "File Management", description = "Endpoint per il caricamento, la gestione e l'eliminazione dei file multimediali e documenti.")
public class FileController {

    @Autowired
    FileService fileService;

    @Operation(
            summary = "Carica un nuovo file",
            description = "Esegue l'upload di un file fisico. Può essere un file generico dell'organizzazione o legato a una lista/categoria specifica."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "File caricato correttamente"),
            @ApiResponse(responseCode = "400", description = "Dati mancanti o file non valido"),
            @ApiResponse(responseCode = "403", description = "Permessi insufficienti per caricare file in questo contesto"),
            @ApiResponse(responseCode = "500", description = "Errore del server durante il salvataggio fisico del file")
    })
    @PostMapping(value = "/upload/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SuccessResponse<FileSummaryDto>> upload(
            @Parameter(description = "Il file binario da caricare", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "ID della Lista opzionale a cui associare il file")
            @RequestParam(value = "list_id", required = false) Long listId,

            @Parameter(description = "ID della Categoria opzionale (es. Documenti, Foto)")
            @RequestParam(value = "category_id", required = false) Long categoryId,

            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        FileSummaryDto result = fileService.uploadFile(file, listId, categoryId, userDetails.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>(
                        true,
                        HttpStatus.CREATED.value(),
                        result,
                        "File Creato con successo",
                        System.currentTimeMillis()
                ));
    }

    @Operation(
            summary = "Elimina un file",
            description = "Rimuove il record dal database e il file fisico dallo storage locale/cloud."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File eliminato correttamente"),
            @ApiResponse(responseCode = "404", description = "File non trovato"),
            @ApiResponse(responseCode = "403", description = "Non hai i permessi per eliminare questo file")
    })
    @DeleteMapping("/delete/")
    public ResponseEntity<SuccessResponse<Object>> delete(
            @Parameter(description = "ID del file da eliminare", required = true)
            @RequestParam(value = "target_file_id") @Valid Long targetId,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();
        fileService.deleteFile(targetId, userId);

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        true,
                        HttpStatus.OK.value(),
                        null,
                        "Eliminazione avvenuta con successo",
                        System.currentTimeMillis()
                )
        );
    }
}