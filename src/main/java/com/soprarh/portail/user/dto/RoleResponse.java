package com.soprarh.portail.user.dto;

import java.util.Set;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        String nom,
        Set<String> permissions
) {}
