package com.soprarh.portail.user.service;

import com.soprarh.portail.shared.BusinessException;
import com.soprarh.portail.user.dto.ChangeRoleRequest;
import com.soprarh.portail.user.dto.UserResponse;
import com.soprarh.portail.user.entity.Role;
import com.soprarh.portail.user.entity.Utilisateur;
import com.soprarh.portail.user.repository.RoleRepository;
import com.soprarh.portail.user.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des utilisateurs.
 * Contient la logique metier pour les operations CRUD et changement de role.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;

    /**
     * Change le role d'un utilisateur.
     * Valide que l'utilisateur existe et que le role est valide.
     *
     * @param userId ID de l'utilisateur
     * @param request contient le nom du nouveau role
     * @return UserResponse avec les informations mises a jour
     * @throws BusinessException si l'utilisateur ou le role n'existe pas
     */
    @Transactional
    public UserResponse changeUserRole(UUID userId, ChangeRoleRequest request) {
        // 1. Verifier que l'utilisateur existe
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        "Utilisateur non trouve avec l'ID: " + userId,
                        HttpStatus.NOT_FOUND));

        // 2. Verifier que le role existe
        String roleName = request.roleName().toUpperCase();
        Role newRole = roleRepository.findByNom(roleName)
                .orElseThrow(() -> new BusinessException(
                        "Role invalide: " + roleName + ". Roles valides: CANDIDAT, MANAGER, RH",
                        HttpStatus.BAD_REQUEST));

        // 3. Mettre a jour les roles (remplacer par le nouveau role)
        Set<Role> newRoles = new HashSet<>();
        newRoles.add(newRole);
        utilisateur.setRoles(newRoles);

        // 4. Sauvegarder
        Utilisateur savedUser = utilisateurRepository.save(utilisateur);

        // 5. Retourner la reponse
        return mapToResponse(savedUser);
    }

    /**
     * Convertit une entite Utilisateur en DTO UserResponse.
     */
    private UserResponse mapToResponse(Utilisateur utilisateur) {
        Set<String> roleNames = utilisateur.getRoles().stream()
                .map(Role::getNom)
                .collect(Collectors.toSet());

        return new UserResponse(
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getEmail(),
                utilisateur.getEtat().name(),
                utilisateur.getTypeUtilisateur().name(),
                roleNames
        );
    }
}

