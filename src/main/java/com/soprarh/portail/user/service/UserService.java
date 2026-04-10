package com.soprarh.portail.user.service;

import com.soprarh.portail.shared.BusinessException;
import com.soprarh.portail.user.dto.ChangeRoleRequest;
import com.soprarh.portail.user.dto.CreateUserRequest;
import com.soprarh.portail.user.dto.UpdateUserRequest;
import com.soprarh.portail.user.dto.UserResponse;
import com.soprarh.portail.user.entity.EtatUtilisateur;
import com.soprarh.portail.user.entity.TypeUtilisateur;
import com.soprarh.portail.user.entity.Role;
import com.soprarh.portail.user.entity.Utilisateur;
import com.soprarh.portail.user.repository.RoleRepository;
import com.soprarh.portail.user.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des utilisateurs.
 * Contient la logique metier pour les operations CRUD et changement de role.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfilService profilService;

    /**
     * Creation d'un utilisateur par le RH.
     * Le RH choisit le type (candidat, manager, rh).
     * Le compte est cree directement comme actif (pas de verification email).
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        // 1. Unicite de l'email
        if (utilisateurRepository.existsByEmail(request.email())) {
            throw new BusinessException(
                    "Un compte existe deja avec cet email.",
                    HttpStatus.CONFLICT);
        }

        // 2. Recherche du role correspondant au type choisi par le RH
        String nomRole = request.typeUtilisateur().name().toUpperCase();
        Role role = roleRepository.findByNom(nomRole)
                .orElseThrow(() -> new BusinessException(
                        "Role non trouve : " + nomRole,
                        HttpStatus.INTERNAL_SERVER_ERROR));

        // 3. Creer l'utilisateur — actif directement
        Utilisateur utilisateur = Utilisateur.builder()
                .nom(request.nom())
                .prenom(request.prenom())
                .email(request.email())
                .motDePasse(passwordEncoder.encode(request.motDePasse()))
                .typeUtilisateur(request.typeUtilisateur())
                .etat(EtatUtilisateur.actif)
                .roles(Set.of(role))
                .build();

        Utilisateur saved = utilisateurRepository.save(utilisateur);
        log.info("Utilisateur cree par RH : id={}, type={}", saved.getId(), saved.getTypeUtilisateur());

        // 4. Creer un profil vide
        profilService.createProfilForNewUser(saved);

        return mapToResponse(saved);
    }

    /**
     * Change le role d'un utilisateur.
     * Valide que l'utilisateur existe et que le role est valide.
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
     * Retourne la liste de tous les utilisateurs.
     * Utilise par GET /api/users (RH uniquement).
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return utilisateurRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Met a jour les informations d'un utilisateur (RH uniquement).
     * PUT /api/users/{id}
     */
    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        "Utilisateur non trouve avec l'ID: " + userId,
                        HttpStatus.NOT_FOUND));

        // Verifier unicite email si change
        if (!utilisateur.getEmail().equalsIgnoreCase(request.email())
                && utilisateurRepository.existsByEmail(request.email())) {
            throw new BusinessException(
                    "Un compte existe deja avec cet email.",
                    HttpStatus.CONFLICT);
        }

        // Verifier que le role existe
        String roleName = request.roleName().toUpperCase();
        Role newRole = roleRepository.findByNom(roleName)
                .orElseThrow(() -> new BusinessException(
                        "Role invalide: " + roleName + ". Roles valides: CANDIDAT, MANAGER, RH",
                        HttpStatus.BAD_REQUEST));

        utilisateur.setNom(request.nom());
        utilisateur.setPrenom(request.prenom());
        utilisateur.setEmail(request.email());
        utilisateur.setEtat(request.etat());

        Set<Role> newRoles = new HashSet<>();
        newRoles.add(newRole);
        utilisateur.setRoles(newRoles);

        Utilisateur saved = utilisateurRepository.save(utilisateur);
        log.info("Utilisateur mis a jour par RH : id={}", saved.getId());
        return mapToResponse(saved);
    }

    /**
     * Retourne la liste des managers actifs.
     * Utilise par GET /api/users/managers.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getManagers() {
        return utilisateurRepository.findByTypeUtilisateur(TypeUtilisateur.manager)
                .stream()
                .filter(u -> u.getEtat() == EtatUtilisateur.actif)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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
                roleNames,
                utilisateur.getDateCreation()
        );
    }
}

