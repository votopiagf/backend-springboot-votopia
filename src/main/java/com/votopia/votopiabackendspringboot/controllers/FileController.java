package com.votopia.votopiabackendspringboot.controllers;

import com.votopia.votopiabackendspringboot.config.CustomUserDetails;
import com.votopia.votopiabackendspringboot.dtos.SuccessResponse;
import com.votopia.votopiabackendspringboot.dtos.file.FileSummaryDto;
import com.votopia.votopiabackendspringboot.services.files.FileService;
import io.swagger.v3.oas.annotations.Operation;
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
public class FileController {
    @Autowired
    FileService fileService;

    @PostMapping(value = "/upload/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Aggiungi File", description="Carica un file e lo associa all'Org o a una Lista.")
    ResponseEntity<SuccessResponse<FileSummaryDto>> upload(
            @RequestParam("file")MultipartFile file,
            @RequestParam(value = "list_id", required = false) Long listId,
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

    @DeleteMapping("/delete/")
    ResponseEntity<SuccessResponse<Object>> delete(@RequestParam(value = "target_file_id") @Valid Long targetId, Authentication authentication){
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
