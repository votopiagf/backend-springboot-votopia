package com.votopia.votopiabackendspringboot.controllers;

import com.votopia.votopiabackendspringboot.config.CustomUserDetails;
import com.votopia.votopiabackendspringboot.dtos.SuccessResponse;
import com.votopia.votopiabackendspringboot.dtos.list.ListCreateDto;
import com.votopia.votopiabackendspringboot.dtos.list.ListSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.list.ListUpdateDto;
import com.votopia.votopiabackendspringboot.entities.List;
import com.votopia.votopiabackendspringboot.services.ListService;
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
public class ListController {
    @Autowired
    private ListService listService;

    @PostMapping("/create/")
    ResponseEntity<SuccessResponse<ListSummaryDto>> create(@RequestBody @Valid ListCreateDto list, Authentication authentication){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        ListSummaryDto listCreated = listService.create(list, userId);
        return new ResponseEntity<>(
                new SuccessResponse<ListSummaryDto>(
                        true,
                        HttpStatus.CREATED.value(),
                        listCreated,
                        "Lista creata con successo",
                        System.currentTimeMillis()
                ), HttpStatus.CREATED
        );
    }

    @PutMapping("/update/")
    ResponseEntity<SuccessResponse<ListSummaryDto>> update(@RequestBody @Valid ListUpdateDto list, Authentication authentication){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        ListSummaryDto listUpdated = listService.update(list, userId);
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

    @GetMapping("/all/")
    ResponseEntity<SuccessResponse<Set<ListSummaryDto>>> getAllVisibile(Authentication authentication){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long idUser = userDetails.getId();

        Set<ListSummaryDto> lists = listService.getAllVisibleLists(idUser);
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
