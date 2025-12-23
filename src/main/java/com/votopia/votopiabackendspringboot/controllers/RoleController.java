package com.votopia.votopiabackendspringboot.controllers;

import com.votopia.votopiabackendspringboot.config.CustomUserDetails;
import com.votopia.votopiabackendspringboot.dtos.SuccessResponse;
import com.votopia.votopiabackendspringboot.dtos.list.ListSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.role.RoleCreateDto;
import com.votopia.votopiabackendspringboot.dtos.role.RoleInfoResponse;
import com.votopia.votopiabackendspringboot.dtos.role.RoleSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.role.RoleUpdateDto;
import com.votopia.votopiabackendspringboot.entities.Role;
import com.votopia.votopiabackendspringboot.services.RoleService;
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
public class RoleController {
    @Autowired
    private RoleService roleService;

    @PostMapping("/create/")
    ResponseEntity<SuccessResponse<RoleSummaryDto>> create(@RequestBody @Valid RoleCreateDto roleCreate, Authentication authentication){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        RoleSummaryDto roleCreated = roleService.create(roleCreate, userId);
        return new ResponseEntity<>(new SuccessResponse<>(
              true,
              201,
              roleCreated,
              "Ruolo creato con successo",
              System.currentTimeMillis()
        ), HttpStatus.CREATED);
    }

    @DeleteMapping("/delete/")
    ResponseEntity<SuccessResponse<Void>> delete(@RequestParam(value = "target_role_id") @Valid Long roleTargetId, Authentication authentication){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        roleService.delete(roleTargetId, userId);
        return ResponseEntity.ok(
                new SuccessResponse<>(
                        true,
                        200,
                        null,
                        "Ruolo eliminato con successo",
                        System.currentTimeMillis()
                )
        );
    }

    @GetMapping("/all/")
    ResponseEntity<SuccessResponse<Set<RoleSummaryDto>>> getAll(@RequestParam(required = false, value = "target_list_id") Long targetListId, Authentication authentication){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        Set<RoleSummaryDto> roles = roleService.getAllVisible(userId, targetListId);
        return ResponseEntity.ok(
                new SuccessResponse<>(
                        true,
                        200,
                        roles,
                        "Ruoli ottenuti con successo",
                        System.currentTimeMillis()
                )
        );
    }

   @GetMapping("/info/")
   ResponseEntity<SuccessResponse<RoleInfoResponse>> info(@RequestParam(value = "target_role_id") Long targetRoleId, Authentication authentication){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        RoleInfoResponse role = roleService.getRoleInformation(userId, targetRoleId);
        return ResponseEntity.ok(
                new SuccessResponse<>(
                        true,
                        200,
                        role,
                        "Ruolo ottenuto con successo",
                        System.currentTimeMillis()
                )
        );
   }

   @PutMapping("/update/")
    ResponseEntity<SuccessResponse<RoleSummaryDto>> update(@RequestBody @Valid RoleUpdateDto role, Authentication authentication){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        RoleSummaryDto roleUpdated = roleService.update(role, userId);
        return ResponseEntity.ok(
                new SuccessResponse<>(
                        true,
                        200,
                        roleUpdated,
                        "Ruolo aggiornato con successo",
                        System.currentTimeMillis()
                )
        );
   }
}
