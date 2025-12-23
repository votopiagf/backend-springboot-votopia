package com.votopia.votopiabackendspringboot.controllers;

import com.votopia.votopiabackendspringboot.config.CustomUserDetails;
import com.votopia.votopiabackendspringboot.dtos.SuccessResponse;
import com.votopia.votopiabackendspringboot.dtos.user.UserCreateDto;
import com.votopia.votopiabackendspringboot.dtos.user.UserSummaryDto;
import com.votopia.votopiabackendspringboot.dtos.user.UserUpdateDto;
import com.votopia.votopiabackendspringboot.services.auth.UserService;
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
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register/")
    ResponseEntity<SuccessResponse<UserSummaryDto>> register(@RequestBody @Valid UserCreateDto user, Authentication authentication){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        UserSummaryDto userRegister = userService.register(user, userId);
        SuccessResponse<UserSummaryDto> response = new SuccessResponse<>(
                true,
                HttpStatus.CREATED.value(), // 201,
                userRegister,
                "Utente registrato con successo",
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/info/")
    ResponseEntity<SuccessResponse<UserSummaryDto>> info(@RequestParam(value = "target_user_id", required = false) Long targetUserId,
                                        Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        UserSummaryDto userTarget = userService.getUserInformation(userId, targetUserId);
        SuccessResponse<UserSummaryDto> response = new SuccessResponse<>(
                true,
                200,
                userTarget,
                "Utente ottenuto con successo",
                System.currentTimeMillis()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all/")
    ResponseEntity<SuccessResponse<Set<UserSummaryDto>>> all(@RequestParam(value = "target_list_id", required = false) Long targetListId, Authentication authentication){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        Set<UserSummaryDto> usersList = userService.getAllVisibleUsers(userId, targetListId);
        SuccessResponse<Set<UserSummaryDto>> response = new SuccessResponse<>(
                true,
                200,
                usersList,
                "Utenti trovati con successo",
                System.currentTimeMillis()
        );
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/delete/")
    ResponseEntity<SuccessResponse<Void>> delete(@RequestParam(value = "target_user_id") Long targetUserId, Authentication authentication){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        userService.delete(targetUserId, userId);
        return ResponseEntity.ok(new SuccessResponse<Void>(
                true,
                200,
                null,
                "Utente eliminato con successo",
                System.currentTimeMillis()
        ));
    }

    @PutMapping("/update/")
    ResponseEntity<SuccessResponse<UserSummaryDto>> update(@RequestBody UserUpdateDto user, Authentication authentication){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        UserSummaryDto userUpdated = userService.update(userId, user);
        return ResponseEntity.ok(new SuccessResponse<UserSummaryDto>(
                true,
                200,
                userUpdated,
                "Utente modificato con successo",
                System.currentTimeMillis()
        ));
    }
}
