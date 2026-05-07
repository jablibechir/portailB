package com.soprarh.portail.user.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record UpdateRolePermissionsRequest(
        @NotNull(message = "La liste des permissions est requise")
        Set<UUID> permissionIds
) {}
