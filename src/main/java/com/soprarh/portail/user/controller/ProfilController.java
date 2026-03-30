package com.soprarh.portail.user.controller;

import com.soprarh.portail.shared.ApiResponse;
import com.soprarh.portail.user.dto.ProfilResponse;
import com.soprarh.portail.user.dto.UpdateProfilRequest;
import com.soprarh.portail.user.entity.Utilisateur;
import com.soprarh.portail.user.service.ProfilService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Controller REST pour la gestion des profils utilisateur.
 * Implemente les endpoints pour US-PROFIL-01, US-PROFIL-02, US-PROFIL-03.
 *
 * Endpoints:
 *   GET    /api/profil           -> consulter son profil
 *   PUT    /api/profil           -> modifier son profil
 *   POST   /api/profil/photo     -> uploader sa photo
 *   GET    /api/profil/photo/{filename} -> recuperer une photo (public)
 */
@RestController
@RequestMapping("/api/profil")
@RequiredArgsConstructor
public class ProfilController {

    private final ProfilService profilService;

    /**
     * US-PROFIL-01: Consulter son profil.
     * GET /api/profil
     * Accessible par tous les utilisateurs connectes.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProfilResponse>> getMonProfil(
            @AuthenticationPrincipal Utilisateur currentUser) {

        ProfilResponse profil = profilService.getMonProfil(currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success(profil, "Profil recupere avec succes"));
    }

    /**
     * US-PROFIL-02: Modifier son profil.
     * PUT /api/profil
     * Accessible par tous les utilisateurs connectes.
     */
    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProfilResponse>> updateProfil(
            @AuthenticationPrincipal Utilisateur currentUser,
            @Valid @RequestBody UpdateProfilRequest request) {

        ProfilResponse profil = profilService.updateProfil(currentUser.getId(), request);

        return ResponseEntity.ok(ApiResponse.success(profil, "Profil mis a jour avec succes"));
    }

    /**
     * US-PROFIL-03: Uploader sa photo de profil.
     * POST /api/profil/photo
     * Accessible par tous les utilisateurs connectes.
     * Accepte: multipart/form-data avec un champ "file"
     */
    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProfilResponse>> uploadPhoto(
            @AuthenticationPrincipal Utilisateur currentUser,
            @RequestParam("file") MultipartFile file) {

        ProfilResponse profil = profilService.uploadPhoto(currentUser.getId(), file);

        return ResponseEntity.ok(ApiResponse.success(profil, "Photo uploadee avec succes"));
    }

    /**
     * Recuperer une photo de profil.
     * GET /api/profil/photo/{filename}
     * Endpoint public pour afficher les photos.
     */
    @GetMapping("/photo/{filename}")
    public ResponseEntity<Resource> getPhoto(@PathVariable String filename) {
        try {
            Path filePath = profilService.getPhotoPath(filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // Determiner le type de contenu
            String contentType;
            try {
                contentType = Files.probeContentType(filePath);
            } catch (IOException e) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

