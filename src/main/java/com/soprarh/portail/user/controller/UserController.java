package com.soprarh.portail.user.controller;

import com.soprarh.portail.shared.ApiResponse;
import com.soprarh.portail.user.dto.ChangeRoleRequest;
import com.soprarh.portail.user.dto.CreateUserRequest;
import com.soprarh.portail.user.dto.UserResponse;
import com.soprarh.portail.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller REST pour la gestion des utilisateurs.
 * Endpoints accessibles par RH uniquement (permission MANAGE_USERS).
 *
 * Endpoints :
 *   POST /api/users           -> creer un utilisateur (RH choisit le type)
 *   PUT  /api/users/{id}/role -> changer le role d'un utilisateur
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Creer un nouvel utilisateur (RH uniquement).
     * Le RH choisit le type: candidat, manager, rh.
     * Le compte est cree directement comme actif (pas de verification email).
     *
     * POST /api/users
     */
    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        UserResponse newUser = userService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(newUser, "Utilisateur cree avec succes"));
    }

    /**
     * Change le role d'un utilisateur.
     *
     * PUT /api/users/{id}/role
     */
    @PutMapping("/{id}/role")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<ApiResponse<UserResponse>> changeUserRole(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeRoleRequest request) {

        UserResponse updatedUser = userService.changeUserRole(id, request);

        return ResponseEntity.ok(ApiResponse.success(
                updatedUser,
                "Role de l'utilisateur mis a jour avec succes"
        ));
    }
}

