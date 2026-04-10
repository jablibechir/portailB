package com.soprarh.portail.evaluation.service;

import com.soprarh.portail.application.entity.Candidature;
import com.soprarh.portail.application.entity.StatutCandidature;
import com.soprarh.portail.application.repository.CandidatureRepository;
import com.soprarh.portail.evaluation.dto.CreateEntretienRequest;
import com.soprarh.portail.evaluation.dto.EntretienResponse;
import com.soprarh.portail.evaluation.entity.Entretien;
import com.soprarh.portail.evaluation.entity.StatutEntretien;
import com.soprarh.portail.evaluation.entity.TypeEntretien;
import com.soprarh.portail.evaluation.mapper.EntretienMapper;
import com.soprarh.portail.evaluation.repository.EntretienRepository;
import com.soprarh.portail.shared.BusinessException;
import com.soprarh.portail.shared.service.NotificationService;
import com.soprarh.portail.user.entity.Utilisateur;
import com.soprarh.portail.user.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service pour la gestion des entretiens.
 * Implemente la planification d'entretiens et declenche les notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EntretienService {

    private final EntretienRepository entretienRepository;
    private final CandidatureRepository candidatureRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EntretienMapper entretienMapper;
    private final NotificationService notificationService;

    /**
     * Planifier un entretien pour une candidature.
     * La candidature doit etre en statut "acceptee_manager".
     * Apres planification, le statut passe a "entretien_planifie".
     *
     * @param request      contient candidatureId, dateEntretien, lieu, type, notes
     * @param planifieParId ID de l'utilisateur qui planifie (RH)
     * @return l'entretien cree
     */
    @Transactional
    public EntretienResponse planifierEntretien(CreateEntretienRequest request, UUID planifieParId) {
        // 1. Verifier que la candidature existe
        Candidature candidature = candidatureRepository.findById(request.candidatureId())
                .orElseThrow(() -> new BusinessException(
                        "Candidature non trouvee avec l'ID: " + request.candidatureId(),
                        HttpStatus.NOT_FOUND));

        // 2. Verifier que le statut permet la planification d'entretien
        if (candidature.getStatut() != StatutCandidature.acceptee_manager) {
            throw new BusinessException(
                    "Impossible de planifier un entretien: la candidature doit etre en statut 'acceptee_manager'. Statut actuel: " 
                    + candidature.getStatut(),
                    HttpStatus.BAD_REQUEST);
        }

        // 3. Verifier que la date est dans le futur
        if (request.dateEntretien().isBefore(LocalDateTime.now())) {
            throw new BusinessException(
                    "La date de l'entretien doit etre dans le futur.",
                    HttpStatus.BAD_REQUEST);
        }

        // 4. Recuperer l'utilisateur qui planifie
        Utilisateur planifiePar = utilisateurRepository.findById(planifieParId)
                .orElseThrow(() -> new BusinessException(
                        "Utilisateur non trouve.", HttpStatus.NOT_FOUND));

        // 4b. Si un interviewer specifique est designe, l'utiliser
        Utilisateur interviewer = planifiePar;
        if (request.interviewerId() != null) {
            interviewer = utilisateurRepository.findById(request.interviewerId())
                    .orElseThrow(() -> new BusinessException(
                            "Interviewer non trouve avec l'ID: " + request.interviewerId(),
                            HttpStatus.NOT_FOUND));
        }

        // 5. Parser le type d'entretien
        TypeEntretien type = parseType(request.type());

        // 6. Creer l'entretien
        Entretien entretien = Entretien.builder()
                .candidature(candidature)
                .planifiePar(interviewer)
                .dateEntretien(request.dateEntretien())
                .lieu(request.lieu())
                .type(type)
                .statut(StatutEntretien.planifie)
                .notes(request.notes())
                .build();

        Entretien saved = entretienRepository.save(entretien);
        log.info("Entretien planifie: entretienId={}, candidatureId={}, date={}", 
                saved.getId(), candidature.getId(), request.dateEntretien());

        // 7. Mettre a jour le statut de la candidature
        candidature.setStatut(StatutCandidature.entretien_planifie);
        candidatureRepository.save(candidature);

        // 8. US-NOTIF-06: Notifier le candidat de l'entretien planifie
        notificationService.notifierEntretienPlanifie(saved);
        // Notification Manager -> entretien planifie
        notificationService.notifierManagerEntretienPlanifie(saved);

        return entretienMapper.toResponse(saved);
    }

    /**
     * Consulter les entretiens d'une candidature.
     *
     * @param candidatureId ID de la candidature
     * @return liste des entretiens
     */
    @Transactional(readOnly = true)
    public List<EntretienResponse> getEntretiensByCandidature(UUID candidatureId) {
        List<Entretien> entretiens = entretienRepository
                .findByCandidatureIdOrderByDateEntretienAsc(candidatureId);
        return entretienMapper.toResponseList(entretiens);
    }

    /**
     * Consulter les entretiens planifies par un utilisateur.
     *
     * @param planifieParId ID de l'utilisateur
     * @return liste des entretiens
     */
    @Transactional(readOnly = true)
    public List<EntretienResponse> getMesEntretiensPlanifies(UUID planifieParId) {
        List<Entretien> entretiens = entretienRepository
                .findByPlanifieParIdOrderByDateEntretienAsc(planifieParId);
        return entretienMapper.toResponseList(entretiens);
    }

    /**
     * Consulter les entretiens a venir pour un candidat.
     *
     * @param candidatId ID du candidat
     * @return liste des entretiens a venir
     */
    @Transactional(readOnly = true)
    public List<EntretienResponse> getMesEntretiensAVenir(UUID candidatId) {
        List<StatutEntretien> statuts = List.of(StatutEntretien.planifie, StatutEntretien.confirme);
        List<Entretien> entretiens = entretienRepository
                .findUpcomingEntretiensByCandidatId(candidatId, LocalDateTime.now(), statuts);
        return entretienMapper.toResponseList(entretiens);
    }

    /**
     * Recuperer un entretien par son ID.
     *
     * @param id ID de l'entretien
     * @return l'entretien
     */
    @Transactional(readOnly = true)
    public EntretienResponse getEntretienById(UUID id) {
        Entretien entretien = entretienRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Entretien non trouve avec l'ID: " + id,
                        HttpStatus.NOT_FOUND));
        return entretienMapper.toResponse(entretien);
    }

    /**
     * US-ENT-05: Calendrier RH — tous les entretiens a venir.
     * Le RH voit l'ensemble des entretiens planifies ou confirmes.
     *
     * @return liste de tous les entretiens a venir
     */
    @Transactional(readOnly = true)
    public List<EntretienResponse> getCalendrierRh() {
        List<StatutEntretien> statuts = List.of(StatutEntretien.planifie, StatutEntretien.confirme);
        List<Entretien> entretiens = entretienRepository
                .findAllUpcomingEntretiens(LocalDateTime.now(), statuts);
        return entretienMapper.toResponseList(entretiens);
    }

    /**
     * US-ENT-05: Calendrier Manager — entretiens a venir du manager connecte.
     * Le Manager voit les entretiens qu'il doit mener (planifie_par = managerId)
     * ou les entretiens des candidatures qui lui sont assignees.
     *
     * @param managerId ID du manager connecte
     * @return liste des entretiens a venir pour ce Manager
     */
    @Transactional(readOnly = true)
    public List<EntretienResponse> getCalendrierManager(UUID managerId) {
        List<StatutEntretien> statuts = List.of(StatutEntretien.planifie, StatutEntretien.confirme);
        List<Entretien> entretiens = entretienRepository
                .findUpcomingEntretiensByManagerId(managerId, LocalDateTime.now(), statuts);
        return entretienMapper.toResponseList(entretiens);
    }

    /**
     * Mettre a jour le statut d'un entretien.
     *
     * @param id ID de l'entretien
     * @param statut nouveau statut (planifie, confirme, annule, termine)
     * @return l'entretien mis a jour
     */
    @Transactional
    public EntretienResponse updateStatut(UUID id, String statut) {
        Entretien entretien = entretienRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Entretien non trouve avec l'ID: " + id,
                        HttpStatus.NOT_FOUND));

        StatutEntretien nouveauStatut = parseStatut(statut);
        entretien.setStatut(nouveauStatut);
        
        Entretien saved = entretienRepository.save(entretien);
        log.info("Statut entretien mis a jour: entretienId={}, statut={}", id, nouveauStatut);

        return entretienMapper.toResponse(saved);
    }

    // ==================== Methodes privees ====================

    private TypeEntretien parseType(String type) {
        if (type == null || type.isBlank()) {
            return TypeEntretien.presentiel; // valeur par defaut
        }
        try {
            return TypeEntretien.valueOf(type.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    "Type d'entretien invalide: " + type + ". Valeurs acceptees: presentiel, visio, telephonique",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private StatutEntretien parseStatut(String statut) {
        try {
            return StatutEntretien.valueOf(statut.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    "Statut d'entretien invalide: " + statut + ". Valeurs acceptees: planifie, confirme, annule, termine",
                    HttpStatus.BAD_REQUEST);
        }
    }
}

