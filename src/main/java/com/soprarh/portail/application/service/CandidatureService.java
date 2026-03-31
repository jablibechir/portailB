package com.soprarh.portail.application.service;

import com.soprarh.portail.application.dto.*;
import com.soprarh.portail.application.entity.Candidature;
import com.soprarh.portail.application.entity.StatutCandidature;
import com.soprarh.portail.application.mapper.CandidatureMapper;
import com.soprarh.portail.application.repository.CandidatureRepository;
import com.soprarh.portail.cv.entity.Cv;
import com.soprarh.portail.cv.repository.CvRepository;
import com.soprarh.portail.offer.entity.OffreEmploi;
import com.soprarh.portail.offer.entity.StatutOffre;
import com.soprarh.portail.offer.repository.OffreEmploiRepository;
import com.soprarh.portail.shared.BusinessException;
import com.soprarh.portail.shared.service.NotificationService;
import com.soprarh.portail.user.entity.Utilisateur;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des candidatures.
 * Implemente les user stories US-CAND-01 a US-CAND-11.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CandidatureService {

    private final CandidatureRepository candidatureRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final OffreEmploiRepository offreRepository;
    private final CvRepository cvRepository;
    private final CandidatureMapper candidatureMapper;
    private final NotificationService notificationService;

    @Value("${app.upload.cv-dir:uploads/cvs}")
    private String cvDir;

    // ==================== Endpoints Candidat ====================

    /**
     * US-CAND-01 & US-CAND-02: Deposer une candidature avec CV optionnel.
     *
     * Regles metier :
     * - L'offre doit exister et etre publiee
     * - Le candidat ne peut postuler qu'une seule fois par offre
     * - Le statut initial est "soumise"
     * - Le CV est optionnel mais s'il est fourni il doit etre un PDF
     *
     * @param request    contient l'ID de l'offre et la lettre de motivation
     * @param cvFile     fichier CV optionnel (PDF)
     * @param candidatId ID du candidat (extrait du token JWT)
     * @return la candidature creee
     */
    @Transactional
    public CandidatureResponse deposerCandidature(CreateCandidatureRequest request, MultipartFile cvFile, UUID candidatId) {
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

        // 6. Si un CV est fourni, le sauvegarder et le lier a la candidature
        if (cvFile != null && !cvFile.isEmpty()) {
            saveCvFile(saved, cvFile);
        }

        // US-NOTIF-02: Notification candidat -> confirmation
        notificationService.notifierCandidatureSoumise(saved);
        // Notification RH -> nouvelle candidature recue
        notificationService.notifierRhNouvelleCandidature(saved);
        // Notification Manager -> nouvelle candidature sur offre
        notificationService.notifierManagerNouvelleCandidatureOffre(saved);

        return candidatureMapper.toResponse(saved);
    }

    /**
     * US-CAND-02 (vue): Consulter ses candidatures.
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
     * US-CAND-07: Transmettre une candidature au Manager.
     * Le RH envoie une candidature en evaluation au Manager pour validation.
     * 
     * Regles metier:
     * - La candidature doit etre en statut "soumise" ou "en_evaluation_rh"
     * - Le statut passe a "envoyee_manager"
     *
     * @param id ID de la candidature
     * @return la candidature mise a jour
     */
    @Transactional
    public CandidatureResponse transmettreAuManager(UUID id) {
        Candidature candidature = findCandidatureOrThrow(id);

        // Verifier que le statut permet la transmission
        if (candidature.getStatut() != StatutCandidature.soumise && 
            candidature.getStatut() != StatutCandidature.en_evaluation_rh) {
            throw new BusinessException(
                    "Impossible de transmettre: la candidature doit etre en statut 'soumise' ou 'en_evaluation_rh'. Statut actuel: " + candidature.getStatut(),
                    HttpStatus.BAD_REQUEST);
        }

        candidature.setStatut(StatutCandidature.envoyee_manager);
        Candidature saved = candidatureRepository.save(candidature);
        log.info("Candidature transmise au manager: id={}", id);

        // US-NOTIF-04: Notifier le candidat que sa candidature progresse
        notificationService.notifierCandidatureEnvoyeeManager(saved);
        // Notification Manager -> nouvelle candidature a evaluer
        notificationService.notifierManagerCandidatureRecue(saved);

        return candidatureMapper.toResponse(saved);
    }

    /**
     * US-CAND-10: Rejeter une candidature (RH).
     * Le RH rejette une candidature qu'il est en train d'evaluer.
     *
     * Regles metier:
     * - La candidature doit etre en statut "soumise" ou "en_evaluation_rh"
     * - Le statut passe a "rejetee_rh"
     *
     * @param id ID de la candidature
     * @return la candidature mise a jour
     */
    @Transactional
    public CandidatureResponse rejeterParRh(UUID id) {
        Candidature candidature = findCandidatureOrThrow(id);

        // Verifier que le statut permet le rejet par RH
        if (candidature.getStatut() != StatutCandidature.soumise && 
            candidature.getStatut() != StatutCandidature.en_evaluation_rh) {
            throw new BusinessException(
                    "Impossible de rejeter: la candidature doit etre en statut 'soumise' ou 'en_evaluation_rh'. Statut actuel: " + candidature.getStatut(),
                    HttpStatus.BAD_REQUEST);
        }

        candidature.setStatut(StatutCandidature.rejetee_rh);
        Candidature saved = candidatureRepository.save(candidature);
        log.info("Candidature rejetee par RH: id={}", id);

        // US-NOTIF-03: Notifier le candidat du rejet
        notificationService.notifierCandidatureRejeteeRh(saved);

        return candidatureMapper.toResponse(saved);
    }

    /**
     * Passer une candidature en evaluation RH.
     * Utilise quand le RH commence a evaluer une candidature soumise.
     *
     * @param id ID de la candidature
     * @return la candidature mise a jour
     */
    @Transactional
    public CandidatureResponse passerEnEvaluationRh(UUID id) {
        Candidature candidature = findCandidatureOrThrow(id);

        if (candidature.getStatut() != StatutCandidature.soumise) {
            throw new BusinessException(
                    "Impossible de passer en evaluation: la candidature doit etre en statut 'soumise'. Statut actuel: " + candidature.getStatut(),
                    HttpStatus.BAD_REQUEST);
        }

        candidature.setStatut(StatutCandidature.en_evaluation_rh);
        Candidature saved = candidatureRepository.save(candidature);
        log.info("Candidature passee en evaluation RH: id={}", id);

        return candidatureMapper.toResponse(saved);
    }

    // ==================== Endpoints Manager ====================

    /**
     * US-CAND-08: Valider une candidature (Manager).
     * Le Manager accepte une candidature qui lui a ete transmise par le RH.
     *
     * Regles metier:
     * - La candidature doit etre en statut "envoyee_manager"
     * - Le statut passe a "acceptee_manager"
     *
     * @param id ID de la candidature
     * @return la candidature mise a jour
     */
    @Transactional
    public CandidatureResponse validerParManager(UUID id) {
        Candidature candidature = findCandidatureOrThrow(id);

        if (candidature.getStatut() != StatutCandidature.envoyee_manager) {
            throw new BusinessException(
                    "Impossible de valider: la candidature doit etre en statut 'envoyee_manager'. Statut actuel: " + candidature.getStatut(),
                    HttpStatus.BAD_REQUEST);
        }

        candidature.setStatut(StatutCandidature.acceptee_manager);
        Candidature saved = candidatureRepository.save(candidature);
        log.info("Candidature validee par manager: id={}", id);

        return candidatureMapper.toResponse(saved);
    }

    /**
     * US-CAND-09: Rejeter une candidature (Manager).
     * Le Manager rejette une candidature qui lui a ete transmise par le RH.
     *
     * Regles metier:
     * - La candidature doit etre en statut "envoyee_manager"
     * - Le statut passe a "rejetee_manager"
     *
     * @param id ID de la candidature
     * @return la candidature mise a jour
     */
    @Transactional
    public CandidatureResponse rejeterParManager(UUID id) {
        Candidature candidature = findCandidatureOrThrow(id);

        if (candidature.getStatut() != StatutCandidature.envoyee_manager) {
            throw new BusinessException(
                    "Impossible de rejeter: la candidature doit etre en statut 'envoyee_manager'. Statut actuel: " + candidature.getStatut(),
                    HttpStatus.BAD_REQUEST);
        }

        candidature.setStatut(StatutCandidature.rejetee_manager);
        Candidature saved = candidatureRepository.save(candidature);
        log.info("Candidature rejetee par manager: id={}", id);

        // US-NOTIF-05: Notifier le candidat du rejet
        notificationService.notifierCandidatureRejeteeManager(saved);
        // Notification RH -> candidature retournee par le manager
        notificationService.notifierRhCandidatureRetourneeManager(saved);

        return candidatureMapper.toResponse(saved);
    }

    /**
     * US-CAND-11: Consulter les candidatures transmises au Manager.
     * Retourne toutes les candidatures dans les statuts lies au Manager.
     *
     * @return liste des candidatures transmises au Manager
     */
    @Transactional(readOnly = true)
    public List<CandidatureResponse> getCandidaturesManager() {
        List<StatutCandidature> statutsManager = List.of(
                StatutCandidature.envoyee_manager,
                StatutCandidature.acceptee_manager,
                StatutCandidature.rejetee_manager,
                StatutCandidature.entretien_planifie
        );

        return candidatureRepository.findByStatutIn(statutsManager)
                .stream()
                .map(candidatureMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ==================== Endpoints existants RH ====================

    /**
     * Modifier le statut generique d'une candidature (RH).
     * @deprecated Utiliser les methodes specifiques (transmettreAuManager, rejeterParRh, etc.)
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
     * Filtrer les candidatures (RH).
     */
    @Transactional(readOnly = true)
    public List<CandidatureResponse> filtrerCandidatures(CandidatureFilterRequest filter) {
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
     * Lister toutes les candidatures (RH).
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

    /**
     * Recupere le chemin physique d'un CV.
     */
    public Path getCvPath(UUID cvId) {
        Cv cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new BusinessException("CV non trouve.", HttpStatus.NOT_FOUND));
        
        Path filePath = Paths.get(cv.getFichier());
        if (!Files.exists(filePath)) {
            throw new BusinessException("Fichier CV non trouve.", HttpStatus.NOT_FOUND);
        }
        return filePath;
    }

    // ==================== Methodes privees ====================

    private Candidature findCandidatureOrThrow(UUID id) {
        return candidatureRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Candidature non trouvee avec l'ID: " + id,
                        HttpStatus.NOT_FOUND));
    }

    private StatutCandidature parseStatut(String statut) {
        try {
            return StatutCandidature.valueOf(statut.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    "Statut invalide: " + statut + ". Valeurs acceptees: " + 
                    java.util.Arrays.toString(StatutCandidature.values()),
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Sauvegarde un fichier CV et le lie a la candidature.
     */
    private void saveCvFile(Candidature candidature, MultipartFile file) {
        // Valider le fichier
        validateCvFile(file);

        try {
            // Creer le repertoire si necessaire
            Path uploadPath = Paths.get(cvDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generer un nom de fichier unique
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String newFilename = candidature.getId() + "_" + System.currentTimeMillis() + extension;

            // Sauvegarder le fichier
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Creer l'entite CV
            Cv cv = Cv.builder()
                    .candidature(candidature)
                    .fichier(filePath.toString())
                    .build();

            cvRepository.save(cv);
            candidature.setCv(cv);
            
            log.info("CV sauvegarde pour candidature: candidatureId={}, fichier={}", 
                    candidature.getId(), filePath);

        } catch (IOException e) {
            log.error("Erreur lors de l'upload du CV: {}", e.getMessage());
            throw new BusinessException(
                    "Erreur lors de l'upload du CV.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateCvFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return; // CV optionnel
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new BusinessException(
                    "Le CV doit etre au format PDF.",
                    HttpStatus.BAD_REQUEST);
        }

        // Limite de taille: 10 MB
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new BusinessException(
                    "La taille du CV ne doit pas depasser 10 MB.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".pdf";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}

