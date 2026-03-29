package com.soprarh.portail.application.service;

import com.soprarh.portail.application.dto.*;
import com.soprarh.portail.application.entity.Candidature;
import com.soprarh.portail.application.entity.StatutCandidature;
import com.soprarh.portail.application.mapper.CandidatureMapper;
import com.soprarh.portail.application.repository.CandidatureRepository;
import com.soprarh.portail.offer.entity.OffreEmploi;
import com.soprarh.portail.offer.entity.StatutOffre;
import com.soprarh.portail.offer.repository.OffreEmploiRepository;
import com.soprarh.portail.shared.BusinessException;
import com.soprarh.portail.user.entity.Utilisateur;
import com.soprarh.portail.user.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des candidatures.
 * Implemente les user stories US-CAND-01 a US-RH-05.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CandidatureService {

    private final CandidatureRepository candidatureRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final OffreEmploiRepository offreRepository;
    private final CandidatureMapper candidatureMapper;

    // ==================== Endpoints Candidat ====================

    /**
     * US-CAND-01: Deposer une candidature.
     *
     * Regles metier :
     * - L'offre doit exister et etre publiee
     * - Le candidat ne peut postuler qu'une seule fois par offre
     * - Le statut initial est "soumise"
     *
     * @param request   contient l'ID de l'offre
     * @param candidatId ID du candidat (extrait du token JWT)
     * @return la candidature creee
     */
    @Transactional
    public CandidatureResponse deposerCandidature(CreateCandidatureRequest request, UUID candidatId) {
        // 1. Verifier que le candidat existe
        Utilisateur candidat = utilisateurRepository.findById(candidatId)
                .orElseThrow(() -> new BusinessException(
                        "Candidat non trouve.", HttpStatus.NOT_FOUND));

        // 2. Verifier que l'offre existe
        OffreEmploi offre = offreRepository.findById(request.offreId())
                .orElseThrow(() -> new BusinessException(
                        "Offre non trouvee avec l'ID: " + request.offreId(),
                        HttpStatus.NOT_FOUND));

        // 3. Verifier que l'offre est publiee
        if (offre.getStatut() != StatutOffre.publiee) {
            throw new BusinessException(
                    "Impossible de postuler: l'offre n'est pas publiee.",
                    HttpStatus.BAD_REQUEST);
        }

        // 4. Verifier l'unicite: un candidat ne peut postuler qu'une fois par offre
        if (candidatureRepository.existsByCandidatIdAndOffreId(candidatId, request.offreId())) {
            throw new BusinessException(
                    "Vous avez deja postule a cette offre.",
                    HttpStatus.CONFLICT);
        }

        // 5. Construire et sauvegarder la candidature
        Candidature candidature = Candidature.builder()
                .candidat(candidat)
                .offre(offre)
                .statut(StatutCandidature.soumise)
                .lettreMotivation(request.lettreMotivation())
                .build();

        Candidature saved = candidatureRepository.save(candidature);
        log.info("Candidature deposee: candidat={}, offre={}", candidatId, request.offreId());

        return candidatureMapper.toResponse(saved);
    }

    /**
     * US-CAND-02: Consulter ses candidatures.
     * Le candidat voit toutes ses candidatures avec leur statut actuel.
     *
     * @param candidatId ID du candidat (extrait du token JWT)
     * @return liste des candidatures du candidat
     */
    @Transactional(readOnly = true)
    public List<CandidatureResponse> mesCandidatures(UUID candidatId) {
        return candidatureRepository.findByCandidatIdOrderByDateSoumissionDesc(candidatId)
                .stream()
                .map(candidatureMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ==================== Endpoints RH ====================

    /**
     * US-CAND-03: Modifier le statut d'une candidature (RH).
     * Statuts valides: soumise, en_evaluation, acceptee, rejetee.
     *
     * @param id      ID de la candidature
     * @param request nouveau statut
     * @return la candidature mise a jour
     */
    @Transactional
    public CandidatureResponse changerStatut(UUID id, ChangeStatutCandidatureRequest request) {
        Candidature candidature = findCandidatureOrThrow(id);

        StatutCandidature nouveauStatut = parseStatut(request.statut());
        candidature.setStatut(nouveauStatut);

        Candidature saved = candidatureRepository.save(candidature);
        log.info("Statut candidature mis a jour: id={}, statut={}", id, nouveauStatut);

        return candidatureMapper.toResponse(saved);
    }

    /**
     * US-RH-04: Filtrer les candidatures (RH).
     * Filtres optionnels: statut, score min, score max.
     *
     * @param filter filtres de recherche
     * @return liste des candidatures correspondantes
     */
    @Transactional(readOnly = true)
    public List<CandidatureResponse> filtrerCandidatures(CandidatureFilterRequest filter) {
        // Parser le statut si fourni
        StatutCandidature statut = null;
        if (filter.statut() != null && !filter.statut().isBlank()) {
            statut = parseStatut(filter.statut());
        }

        return candidatureRepository.findByFilters(statut, filter.scoreMin(), filter.scoreMax())
                .stream()
                .map(candidatureMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * US-RH-05: Consulter toutes les candidatures (RH).
     * Tri par date (defaut) ou par score.
     *
     * @param tri "score" pour trier par score, sinon tri par date
     * @return liste de toutes les candidatures
     */
    @Transactional(readOnly = true)
    public List<CandidatureResponse> listerCandidatures(String tri) {
        List<Candidature> candidatures;

        if ("score".equalsIgnoreCase(tri)) {
            candidatures = candidatureRepository.findAllOrderByScoreDesc();
        } else {
            candidatures = candidatureRepository.findAllOrderByDateDesc();
        }

        return candidatures.stream()
                .map(candidatureMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Recuperer une candidature par ID.
     */
    @Transactional(readOnly = true)
    public CandidatureResponse getCandidatureById(UUID id) {
        Candidature candidature = findCandidatureOrThrow(id);
        return candidatureMapper.toResponse(candidature);
    }

    // ==================== Methodes utilitaires ====================

    /**
     * Trouve une candidature par ID ou lance une exception.
     */
    private Candidature findCandidatureOrThrow(UUID id) {
        return candidatureRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Candidature non trouvee avec l'ID: " + id,
                        HttpStatus.NOT_FOUND));
    }

    /**
     * Parse une chaine en StatutCandidature.
     */
    private StatutCandidature parseStatut(String statut) {
        try {
            return StatutCandidature.valueOf(statut.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    "Statut invalide: " + statut + ". Valeurs acceptees: soumise, en_evaluation, acceptee, rejetee",
                    HttpStatus.BAD_REQUEST);
        }
    }
}

