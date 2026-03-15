package com.soprarh.portail.user.controller;

import com.soprarh.portail.shared.ApiResponse;
import com.soprarh.portail.user.dto.ChangeRoleRequest;
import com.soprarh.portail.user.dto.UserResponse;
import com.soprarh.portail.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller REST pour la gestion des utilisateurs.
 * Endpoints accessibles par Admin (RH) et Manager uniquement.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Change le role d'un utilisateur.
     *
     * PUT /api/users/{id}/role
     *
     * Accessible uniquement par RH (MANAGE_USERS) ou Manager (via permissions).
     * Les roles valides sont: CANDIDAT, MANAGER, RH
     *
     * @param id ID de l'utilisateur
     * @param request contient le nouveau role
     * @return UserResponse avec les informations mises a jour
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

