package com.votopia.votopiabackendspringboot.controllers;

import com.votopia.votopiabackendspringboot.config.CustomUserDetails;
import com.votopia.votopiabackendspringboot.dtos.SuccessResponse;
import com.votopia.votopiabackendspringboot.dtos.file.FileSummaryDto;
import com.votopia.votopiabackendspringboot.services.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/upload/")

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
}
