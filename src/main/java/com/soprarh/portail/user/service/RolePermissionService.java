package com.soprarh.portail.user.service;

import com.soprarh.portail.shared.BusinessException;
import com.soprarh.portail.user.dto.PermissionResponse;
import com.soprarh.portail.user.dto.RoleResponse;
import com.soprarh.portail.user.dto.UpdateRolePermissionsRequest;
import com.soprarh.portail.user.entity.Permission;
import com.soprarh.portail.user.entity.Role;
import com.soprarh.portail.user.repository.PermissionRepository;
import com.soprarh.portail.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RolePermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::mapRoleToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::mapPermissionToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoleResponse updateRolePermissions(UUID roleId, UpdateRolePermissionsRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(
                        "Role non trouve avec l'ID: " + roleId,
                        HttpStatus.NOT_FOUND));

        List<Permission> permissions = permissionRepository.findAllById(request.permissionIds());
        if (permissions.size() != request.permissionIds().size()) {
            throw new BusinessException(
                    "Certaines permissions sont invalides.",
                    HttpStatus.BAD_REQUEST);
        }

        role.setPermissions(new HashSet<>(permissions));
        Role saved = roleRepository.save(role);
        log.info("Permissions du role {} mises a jour: {}", saved.getNom(),
                permissions.stream().map(Permission::getCode).collect(Collectors.joining(", ")));

        return mapRoleToResponse(saved);
    }

    private RoleResponse mapRoleToResponse(Role role) {
        Set<String> permissionCodes = role.getPermissions().stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());
        return new RoleResponse(role.getId(), role.getNom(), permissionCodes);
    }

    private PermissionResponse mapPermissionToResponse(Permission permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getCode(),
                permission.getDescription()
        );
    }
}
