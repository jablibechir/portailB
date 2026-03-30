package com.soprarh.portail.user.service;

import com.soprarh.portail.shared.BusinessException;
import com.soprarh.portail.user.dto.ProfilResponse;
import com.soprarh.portail.user.dto.UpdateProfilRequest;
import com.soprarh.portail.user.entity.Profil;
import com.soprarh.portail.user.entity.Utilisateur;
import com.soprarh.portail.user.mapper.ProfilMapper;
import com.soprarh.portail.user.repository.ProfilRepository;
import com.soprarh.portail.user.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service pour la gestion des profils utilisateur.
 * Implemente US-PROFIL-01, US-PROFIL-02, US-PROFIL-03.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfilService {

    private final ProfilRepository profilRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ProfilMapper profilMapper;

    @Value("${app.upload.photos-dir:uploads/photos}")
    private String photosDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * US-PROFIL-01: Consulter son profil.
     * Si le profil n'existe pas encore, retourne un profil vide avec les infos utilisateur.
     *
     * @param utilisateurId ID de l'utilisateur connecte
     * @return le profil de l'utilisateur
     */
    @Transactional(readOnly = true)
    public ProfilResponse getMonProfil(UUID utilisateurId) {
        Utilisateur utilisateur = findUtilisateurOrThrow(utilisateurId);

        // Chercher le profil existant ou creer une reponse vide
        return profilRepository.findByUtilisateurId(utilisateurId)
                .map(profilMapper::toResponse)
                .orElseGet(() -> createEmptyProfilResponse(utilisateur));
    }

    /**
     * US-PROFIL-02: Modifier son profil.
     * Met a jour telephone et adresse.
     *
     * @param utilisateurId ID de l'utilisateur connecte
     * @param request       nouvelles donnees
     * @return le profil mis a jour
     */
    @Transactional
    public ProfilResponse updateProfil(UUID utilisateurId, UpdateProfilRequest request) {
        Utilisateur utilisateur = findUtilisateurOrThrow(utilisateurId);

        // Trouver ou creer le profil
        Profil profil = profilRepository.findByUtilisateurId(utilisateurId)
                .orElseGet(() -> createEmptyProfil(utilisateur));

        // Mise a jour des champs fournis
        if (request.telephone() != null) {
            profil.setTelephone(request.telephone());
        }
        if (request.adresse() != null) {
            profil.setAdresse(request.adresse());
        }
        profil.setDateMiseAJour(LocalDateTime.now());

        Profil saved = profilRepository.save(profil);
        log.info("Profil mis a jour pour utilisateur: {}", utilisateurId);

        return profilMapper.toResponse(saved);
    }

    /**
     * US-PROFIL-03: Uploader une photo de profil.
     * Sauvegarde le fichier et stocke le chemin.
     *
     * @param utilisateurId ID de l'utilisateur connecte
     * @param file          fichier image uploade
     * @return le profil avec la nouvelle photo
     */
    @Transactional
    public ProfilResponse uploadPhoto(UUID utilisateurId, MultipartFile file) {
        Utilisateur utilisateur = findUtilisateurOrThrow(utilisateurId);

        // Valider le fichier
        validateImageFile(file);

        // Trouver ou creer le profil
        Profil profil = profilRepository.findByUtilisateurId(utilisateurId)
                .orElseGet(() -> createEmptyProfil(utilisateur));

        // Generer un nom de fichier unique
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String newFilename = utilisateurId + "_" + System.currentTimeMillis() + extension;

        // Sauvegarder le fichier
        try {
            Path uploadPath = Paths.get(photosDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Supprimer l'ancienne photo si elle existe
            deleteOldPhoto(profil.getPhotoUrl());

            // Mettre a jour le profil avec l'URL de la photo
            String photoUrl = "/api/profil/photo/" + newFilename;
            profil.setPhotoUrl(photoUrl);
            profil.setDateMiseAJour(LocalDateTime.now());

            Profil saved = profilRepository.save(profil);
            log.info("Photo de profil uploadee pour utilisateur: {}", utilisateurId);

            return profilMapper.toResponse(saved);

        } catch (IOException e) {
            log.error("Erreur lors de l'upload de la photo: {}", e.getMessage());
            throw new BusinessException(
                    "Erreur lors de l'upload de la photo.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Recupere le chemin physique d'une photo.
     *
     * @param filename nom du fichier
     * @return le chemin vers le fichier
     */
    public Path getPhotoPath(String filename) {
        Path filePath = Paths.get(photosDir).resolve(filename);
        if (!Files.exists(filePath)) {
            throw new BusinessException("Photo non trouvee.", HttpStatus.NOT_FOUND);
        }
        return filePath;
    }

    /**
     * Cree un profil vide pour un nouvel utilisateur.
     * Appele lors de l'inscription.
     *
     * @param utilisateur l'utilisateur nouvellement cree
     * @return le profil cree
     */
    @Transactional
    public Profil createProfilForNewUser(Utilisateur utilisateur) {
        Profil profil = Profil.builder()
                .utilisateur(utilisateur)
                .build();
        return profilRepository.save(profil);
    }

    // ==================== Methodes privees ====================

    private Utilisateur findUtilisateurOrThrow(UUID utilisateurId) {
        return utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new BusinessException(
                        "Utilisateur non trouve.",
                        HttpStatus.NOT_FOUND
                ));
    }

    private Profil createEmptyProfil(Utilisateur utilisateur) {
        Profil profil = Profil.builder()
                .utilisateur(utilisateur)
                .build();
        return profilRepository.save(profil);
    }

    private ProfilResponse createEmptyProfilResponse(Utilisateur utilisateur) {
        return new ProfilResponse(
                null,
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getEmail(),
                null,
                null,
                null,
                null
        );
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Le fichier est vide.", HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(
                    "Le fichier doit etre une image (JPEG, PNG, etc.).",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Limite de taille: 5 MB
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new BusinessException(
                    "La taille du fichier ne doit pas depasser 5 MB.",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private void deleteOldPhoto(String photoUrl) {
        if (photoUrl == null || photoUrl.isBlank()) {
            return;
        }
        try {
            String filename = photoUrl.substring(photoUrl.lastIndexOf("/") + 1);
            Path oldFilePath = Paths.get(photosDir).resolve(filename);
            Files.deleteIfExists(oldFilePath);
        } catch (IOException e) {
            log.warn("Impossible de supprimer l'ancienne photo: {}", e.getMessage());
        }
    }
}

