package com.votopia.votopiabackendspringboot.controllers;

import com.votopia.votopiabackendspringboot.dtos.SuccessResponse;
import com.votopia.votopiabackendspringboot.dtos.organization.OrganizationSummaryDto;
import com.votopia.votopiabackendspringboot.services.OrganizationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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
public class OrganizationController {
    @Autowired
    private OrganizationService organizationService;

    @GetMapping("/by-code/")
    @SecurityRequirements
    ResponseEntity<SuccessResponse<OrganizationSummaryDto>> getByCode(@RequestParam(value = "organization_id") @Valid String code){
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
