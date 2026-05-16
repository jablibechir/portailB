package com.soprarh.portail.user.controller;

import com.soprarh.portail.shared.ApiResponse;
import com.soprarh.portail.user.dto.PermissionResponse;
import com.soprarh.portail.user.dto.RoleResponse;
import com.soprarh.portail.user.dto.UpdateRolePermissionsRequest;
import com.soprarh.portail.user.service.RolePermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST pour la gestion des roles et permissions.
 * Accessible par RH uniquement (permission GERER_UTILISATEURS).
 *
 * Endpoints :
 *   GET  /api/roles              -> lister tous les roles avec leurs permissions
 *   GET  /api/permissions        -> lister toutes les permissions disponibles
 *   PUT  /api/roles/{id}/permissions -> modifier les permissions d'un role
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('GERER_UTILISATEURS')")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        List<RoleResponse> roles = rolePermissionService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.success(roles, "Roles trouves: " + roles.size()));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('GERER_UTILISATEURS')")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions() {
        List<PermissionResponse> permissions = rolePermissionService.getAllPermissions();
        return ResponseEntity.ok(ApiResponse.success(permissions, "Permissions trouvees: " + permissions.size()));
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('GERER_UTILISATEURS')")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRolePermissions(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRolePermissionsRequest request) {
        RoleResponse updated = rolePermissionService.updateRolePermissions(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Permissions du role mises a jour avec succes"));
    }
}
