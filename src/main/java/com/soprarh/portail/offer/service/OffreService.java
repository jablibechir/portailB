package com.soprarh.portail.offer.service;

import com.soprarh.portail.offer.dto.*;
import com.soprarh.portail.offer.entity.OffreEmploi;
import com.soprarh.portail.offer.entity.StatutOffre;
import com.soprarh.portail.offer.mapper.OffreMapper;
import com.soprarh.portail.offer.repository.OffreEmploiRepository;
import com.soprarh.portail.shared.BusinessException;
import com.soprarh.portail.user.entity.Utilisateur;
import com.soprarh.portail.user.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des offres d'emploi.
 * Implemente les user stories US-OFF-01 a US-OFF-05.
 */
@Service
@RequiredArgsConstructor
public class OffreService {

    private final OffreEmploiRepository offreRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final OffreMapper offreMapper;

    /**
     * US-OFF-01: Creer une offre d'emploi.
     * Statut initial: brouillon ou publiee selon le choix.
     *
     * @param request   donnees de l'offre
     * @param createurId ID de l'utilisateur qui cree l'offre
     * @return l'offre creee
     */
    @Transactional
    public OffreResponse createOffre(CreateOffreRequest request, UUID createurId) {
        // Recuperer l'utilisateur createur
        Utilisateur createur = utilisateurRepository.findById(createurId)
                .orElseThrow(() -> new BusinessException(
                        "Utilisateur non trouve", HttpStatus.NOT_FOUND));

        // Determiner le statut initial
        StatutOffre statut = StatutOffre.brouillon;
        if (request.statut() != null && !request.statut().isBlank()) {
            statut = parseStatut(request.statut());
        }

        // Construire l'offre
        OffreEmploi offre = OffreEmploi.builder()
                .titre(request.titre())
                .description(request.description())
                .competencesRequises(request.competencesRequises())
                .experienceRequise(request.experienceRequise())
                .formationRequise(request.formationRequise())
                .statut(statut)
                .datePublication(statut == StatutOffre.publiee ? LocalDate.now() : null)
                .creePar(createur)
                .build();

        OffreEmploi saved = offreRepository.save(offre);
        return offreMapper.toResponse(saved);
    }

    /**
     * US-OFF-02: Modifier une offre d'emploi.
     * Seuls les champs non-null sont mis a jour.
     *
     * @param id      ID de l'offre
     * @param request nouvelles donnees
     * @return l'offre mise a jour
     */
    @Transactional
    public OffreResponse updateOffre(UUID id, UpdateOffreRequest request) {
        OffreEmploi offre = findOffreOrThrow(id);

        // Mise a jour partielle: seuls les champs fournis sont modifies
        if (request.titre() != null && !request.titre().isBlank()) {
            offre.setTitre(request.titre());
        }
        if (request.description() != null) {
            offre.setDescription(request.description());
        }
        if (request.competencesRequises() != null) {
            offre.setCompetencesRequises(request.competencesRequises());
        }
        if (request.experienceRequise() != null) {
            offre.setExperienceRequise(request.experienceRequise());
        }
        if (request.formationRequise() != null) {
            offre.setFormationRequise(request.formationRequise());
        }

        OffreEmploi saved = offreRepository.save(offre);
        return offreMapper.toResponse(saved);
    }

    /**
     * US-OFF-03: Supprimer une offre d'emploi.
     * La confirmation est geree cote frontend.
     * Suppression definitive en base.
     *
     * @param id ID de l'offre a supprimer
     */
    @Transactional
    public void deleteOffre(UUID id) {
        OffreEmploi offre = findOffreOrThrow(id);
        offreRepository.delete(offre);
    }

    /**
     * US-OFF-04: Changer le statut d'une offre.
     * Statuts valides: brouillon, publiee, archivee.
     * Si publiee, met a jour la date de publication.
     *
     * @param id      ID de l'offre
     * @param request nouveau statut
     * @return l'offre mise a jour
     */
    @Transactional
    public OffreResponse changeStatut(UUID id, ChangeStatutRequest request) {
        OffreEmploi offre = findOffreOrThrow(id);

        StatutOffre nouveauStatut = parseStatut(request.statut());
        offre.setStatut(nouveauStatut);

        // Si on publie l'offre, mettre a jour la date de publication
        if (nouveauStatut == StatutOffre.publiee && offre.getDatePublication() == null) {
            offre.setDatePublication(LocalDate.now());
        }

        OffreEmploi saved = offreRepository.save(offre);
        return offreMapper.toResponse(saved);
    }

    /**
     * US-OFF-05: Filtrer les offres publiees.
     * Accessible aux candidats.
     * Retourne uniquement les offres avec statut = publiee.
     *
     * @param filter filtres optionnels
     * @return liste des offres correspondantes
     */
    @Transactional(readOnly = true)
    public List<OffreResponse> searchOffresPubliees(OffreFilterRequest filter) {
        String keyword = (filter != null && filter.keyword() != null && !filter.keyword().isBlank())
                ? filter.keyword() : null;
        String experience = (filter != null && filter.experience() != null && !filter.experience().isBlank())
                ? filter.experience() : null;
        String formation = (filter != null && filter.formation() != null && !filter.formation().isBlank())
                ? filter.formation() : null;

        List<OffreEmploi> offres = offreRepository.findByFilters(
                StatutOffre.publiee, keyword, experience, formation);

        return offres.stream()
                .map(offreMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Recuperer toutes les offres (pour Admin/RH).
     * Inclut tous les statuts.
     */
    @Transactional(readOnly = true)
    public List<OffreResponse> getAllOffres() {
        return offreRepository.findAll().stream()
                .map(offreMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Recuperer une offre par son ID.
     */
    @Transactional(readOnly = true)
    public OffreResponse getOffreById(UUID id) {
        OffreEmploi offre = findOffreOrThrow(id);
        return offreMapper.toResponse(offre);
    }

    // ==================== Methodes utilitaires ====================

    /**
     * Trouve une offre par ID ou lance une exception.
     */
    private OffreEmploi findOffreOrThrow(UUID id) {
        return offreRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Offre non trouvee avec l'ID: " + id,
                        HttpStatus.NOT_FOUND));
    }

    /**
     * Parse une chaine en StatutOffre.
     */
    private StatutOffre parseStatut(String statut) {
        try {
            return StatutOffre.valueOf(statut.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    "Statut invalide: " + statut + ". Valeurs acceptees: brouillon, publiee, archivee",
                    HttpStatus.BAD_REQUEST);
        }
    }
}

