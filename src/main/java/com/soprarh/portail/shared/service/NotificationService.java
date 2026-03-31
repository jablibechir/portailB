package com.soprarh.portail.shared.service;

import com.soprarh.portail.application.entity.Candidature;
import com.soprarh.portail.evaluation.entity.Entretien;
import com.soprarh.portail.offer.entity.OffreEmploi;
import com.soprarh.portail.shared.dto.NotificationResponse;
import com.soprarh.portail.shared.entity.Notification;
import com.soprarh.portail.shared.entity.TypeNotification;
import com.soprarh.portail.shared.mapper.NotificationMapper;
import com.soprarh.portail.shared.repository.NotificationRepository;
import com.soprarh.portail.user.entity.TypeUtilisateur;
import com.soprarh.portail.user.entity.Utilisateur;
import com.soprarh.portail.user.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Service pour la gestion des notifications.
 * Gere les notifications automatiques pour Candidat, RH et Manager.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final UtilisateurRepository utilisateurRepository;

    // ================================================================
    //                  NOTIFICATIONS CANDIDAT
    // ================================================================

    /**
     * US-NOTIF-01: Notification lors de l'activation du compte.
     */
    @Transactional
    public void notifierActivationCompte(Utilisateur utilisateur) {
        Notification notification = Notification.builder()
                .utilisateur(utilisateur)
                .titre("Bienvenue sur votre espace candidat")
                .message("Votre compte a ete active avec succes. Vous pouvez maintenant postuler aux offres disponibles.")
                .type(TypeNotification.activation_compte)
                .build();

        notificationRepository.save(notification);
        log.info("Notification activation compte creee pour utilisateur: {}", utilisateur.getId());
    }

    /**
     * US-NOTIF-02: Notification lors de la soumission d'une candidature.
     */
    @Transactional
    public void notifierCandidatureSoumise(Candidature candidature) {
        Notification notification = Notification.builder()
                .utilisateur(candidature.getCandidat())
                .titre("Candidature envoyee avec succes !")
                .message("Votre candidature pour le poste \"" + candidature.getOffre().getTitre()
                        + "\" a ete soumise. Vous serez informe de l'avancement de votre dossier.")
                .type(TypeNotification.candidature_soumise)
                .build();

        notificationRepository.save(notification);
        log.info("Notification candidature soumise creee pour candidat: {}", candidature.getCandidat().getId());
    }

    /**
     * US-NOTIF-03: Notification lors du rejet par le RH.
     */
    @Transactional
    public void notifierCandidatureRejeteeRh(Candidature candidature) {
        Notification notification = Notification.builder()
                .utilisateur(candidature.getCandidat())
                .titre("Mise a jour de votre candidature")
                .message("Nous vous informons que votre candidature pour le poste \""
                        + candidature.getOffre().getTitre()
                        + "\" n'a pas ete retenue. Nous vous souhaitons bonne chance pour vos futures recherches.")
                .type(TypeNotification.candidature_rejetee_rh)
                .build();

        notificationRepository.save(notification);
        log.info("Notification rejet RH creee pour candidat: {}", candidature.getCandidat().getId());
    }

    /**
     * US-NOTIF-04: Notification lors de la transmission au Manager.
     */
    @Transactional
    public void notifierCandidatureEnvoyeeManager(Candidature candidature) {
        Notification notification = Notification.builder()
                .utilisateur(candidature.getCandidat())
                .titre("Votre candidature progresse !")
                .message("Bonne nouvelle ! Votre candidature pour le poste \""
                        + candidature.getOffre().getTitre()
                        + "\" a ete transmise au manager pour evaluation.")
                .type(TypeNotification.candidature_envoyee_manager)
                .build();

        notificationRepository.save(notification);
        log.info("Notification envoyee manager creee pour candidat: {}", candidature.getCandidat().getId());
    }

    /**
     * US-NOTIF-05: Notification lors du rejet par le Manager.
     */
    @Transactional
    public void notifierCandidatureRejeteeManager(Candidature candidature) {
        Notification notification = Notification.builder()
                .utilisateur(candidature.getCandidat())
                .titre("Mise a jour de votre candidature")
                .message("Nous vous informons que votre candidature pour le poste \""
                        + candidature.getOffre().getTitre()
                        + "\" n'a pas ete retenue apres evaluation. Nous vous remercions de l'interet porte a notre entreprise.")
                .type(TypeNotification.candidature_rejetee_manager)
                .build();

        notificationRepository.save(notification);
        log.info("Notification rejet Manager creee pour candidat: {}", candidature.getCandidat().getId());
    }

    /**
     * US-NOTIF-06: Notification lors de la planification d'un entretien.
     */
    @Transactional
    public void notifierEntretienPlanifie(Entretien entretien) {
        Candidature candidature = entretien.getCandidature();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy a HH:mm");
        String dateFormatted = entretien.getDateEntretien().format(formatter);

        String lieuInfo = entretien.getLieu() != null ? " Lieu: " + entretien.getLieu() + "." : "";
        String typeInfo = entretien.getType() != null ? " Type: " + entretien.getType() + "." : "";

        Notification notification = Notification.builder()
                .utilisateur(candidature.getCandidat())
                .titre("Entretien planifie")
                .message("Un entretien a ete planifie pour votre candidature au poste \""
                        + candidature.getOffre().getTitre()
                        + "\" le " + dateFormatted + "." + typeInfo + lieuInfo)
                .type(TypeNotification.entretien_planifie)
                .build();

        notificationRepository.save(notification);
        log.info("Notification entretien planifie creee pour candidat: {}", candidature.getCandidat().getId());
    }

    // ================================================================
    //                  NOTIFICATIONS RH
    // ================================================================

    /**
     * Notifier tous les RH qu'une nouvelle candidature a ete recue.
     * Declenchee automatiquement quand un candidat soumet une candidature.
     */
    @Transactional
    public void notifierRhNouvelleCandidature(Candidature candidature) {
        String candidatNom = candidature.getCandidat().getPrenom() + " " + candidature.getCandidat().getNom();
        String offreTitre = candidature.getOffre().getTitre();

        List<Utilisateur> rhUsers = utilisateurRepository.findByTypeUtilisateur(TypeUtilisateur.rh);
        for (Utilisateur rh : rhUsers) {
            Notification notification = Notification.builder()
                    .utilisateur(rh)
                    .titre("Nouvelle candidature recue")
                    .message("Une nouvelle candidature a ete soumise par " + candidatNom
                            + " pour le poste \"" + offreTitre + "\".")
                    .type(TypeNotification.nouvelle_candidature_recue)
                    .build();
            notificationRepository.save(notification);
        }
        log.info("Notification nouvelle candidature envoyee a {} RH pour offre: {}", rhUsers.size(), offreTitre);
    }

    /**
     * Notifier tous les RH qu'un Manager a evalue une candidature.
     * Declenchee automatiquement quand le Manager fait une evaluation.
     */
    @Transactional
    public void notifierRhEvaluationManager(Candidature candidature, Utilisateur manager, String decision) {
        String candidatNom = candidature.getCandidat().getPrenom() + " " + candidature.getCandidat().getNom();
        String offreTitre = candidature.getOffre().getTitre();
        String managerNom = manager.getPrenom() + " " + manager.getNom();

        List<Utilisateur> rhUsers = utilisateurRepository.findByTypeUtilisateur(TypeUtilisateur.rh);
        for (Utilisateur rh : rhUsers) {
            Notification notification = Notification.builder()
                    .utilisateur(rh)
                    .titre("Evaluation Manager recue")
                    .message("Le manager " + managerNom + " a evalue la candidature de " + candidatNom
                            + " pour le poste \"" + offreTitre + "\". Decision: " + decision + ".")
                    .type(TypeNotification.evaluation_manager_recue)
                    .build();
            notificationRepository.save(notification);
        }
        log.info("Notification evaluation manager envoyee a {} RH", rhUsers.size());
    }

    /**
     * Notifier tous les RH qu'un Manager a rejete une candidature.
     * La candidature est retournee au RH pour traitement.
     */
    @Transactional
    public void notifierRhCandidatureRetourneeManager(Candidature candidature) {
        String candidatNom = candidature.getCandidat().getPrenom() + " " + candidature.getCandidat().getNom();
        String offreTitre = candidature.getOffre().getTitre();

        List<Utilisateur> rhUsers = utilisateurRepository.findByTypeUtilisateur(TypeUtilisateur.rh);
        for (Utilisateur rh : rhUsers) {
            Notification notification = Notification.builder()
                    .utilisateur(rh)
                    .titre("Candidature retournee par le Manager")
                    .message("La candidature de " + candidatNom
                            + " pour le poste \"" + offreTitre + "\" a ete rejetee par le manager et necessite votre attention.")
                    .type(TypeNotification.candidature_retournee_manager)
                    .build();
            notificationRepository.save(notification);
        }
        log.info("Notification candidature retournee envoyee a {} RH", rhUsers.size());
    }

    /**
     * Notifier tous les RH qu'une offre expire bientot.
     */
    @Transactional
    public void notifierRhOffreExpireBientot(OffreEmploi offre) {
        List<Utilisateur> rhUsers = utilisateurRepository.findByTypeUtilisateur(TypeUtilisateur.rh);
        for (Utilisateur rh : rhUsers) {
            Notification notification = Notification.builder()
                    .utilisateur(rh)
                    .titre("Offre expire bientot")
                    .message("L'offre \"" + offre.getTitre() + "\" expire le " + offre.getDateExpiration()
                            + ". Pensez a la prolonger ou l'archiver.")
                    .type(TypeNotification.offre_expire_bientot)
                    .build();
            notificationRepository.save(notification);
        }
        log.info("Notification offre expire bientot envoyee a {} RH pour offre: {}", rhUsers.size(), offre.getTitre());
    }

    /**
     * Notifier tous les RH qu'un nouveau compte utilisateur a ete cree.
     * Declenchee automatiquement lors de l'inscription d'un utilisateur.
     */
    @Transactional
    public void notifierRhNouveauCompte(Utilisateur nouvelUtilisateur) {
        String nom = nouvelUtilisateur.getPrenom() + " " + nouvelUtilisateur.getNom();
        String type = nouvelUtilisateur.getTypeUtilisateur().name();

        List<Utilisateur> rhUsers = utilisateurRepository.findByTypeUtilisateur(TypeUtilisateur.rh);
        for (Utilisateur rh : rhUsers) {
            Notification notification = Notification.builder()
                    .utilisateur(rh)
                    .titre("Nouveau compte cree")
                    .message("Un nouveau compte " + type + " a ete cree : " + nom
                            + " (" + nouvelUtilisateur.getEmail() + ").")
                    .type(TypeNotification.nouveau_compte_cree)
                    .build();
            notificationRepository.save(notification);
        }
        log.info("Notification nouveau compte envoyee a {} RH pour: {}", rhUsers.size(), nom);
    }

    // ================================================================
    //                  NOTIFICATIONS MANAGER
    // ================================================================

    /**
     * Notifier tous les Managers qu'une candidature leur a ete transmise.
     * Declenchee automatiquement quand le RH transmet une candidature.
     */
    @Transactional
    public void notifierManagerCandidatureRecue(Candidature candidature) {
        String candidatNom = candidature.getCandidat().getPrenom() + " " + candidature.getCandidat().getNom();
        String offreTitre = candidature.getOffre().getTitre();

        List<Utilisateur> managers = utilisateurRepository.findByTypeUtilisateur(TypeUtilisateur.manager);
        for (Utilisateur manager : managers) {
            Notification notification = Notification.builder()
                    .utilisateur(manager)
                    .titre("Nouvelle candidature a evaluer")
                    .message("Le RH vous a transmis la candidature de " + candidatNom
                            + " pour le poste \"" + offreTitre + "\". Veuillez proceder a l'evaluation.")
                    .type(TypeNotification.candidature_recue_pour_evaluation)
                    .build();
            notificationRepository.save(notification);
        }
        log.info("Notification candidature recue envoyee a {} Managers", managers.size());
    }

    /**
     * Notifier tous les Managers qu'une nouvelle candidature a ete soumise sur une offre.
     * Declenchee automatiquement quand un candidat soumet une candidature.
     */
    @Transactional
    public void notifierManagerNouvelleCandidatureOffre(Candidature candidature) {
        String candidatNom = candidature.getCandidat().getPrenom() + " " + candidature.getCandidat().getNom();
        String offreTitre = candidature.getOffre().getTitre();

        List<Utilisateur> managers = utilisateurRepository.findByTypeUtilisateur(TypeUtilisateur.manager);
        for (Utilisateur manager : managers) {
            Notification notification = Notification.builder()
                    .utilisateur(manager)
                    .titre("Nouvelle candidature sur une offre")
                    .message("Une nouvelle candidature a ete soumise par " + candidatNom
                            + " pour le poste \"" + offreTitre + "\".")
                    .type(TypeNotification.nouvelle_candidature_offre_assignee)
                    .build();
            notificationRepository.save(notification);
        }
        log.info("Notification nouvelle candidature offre envoyee a {} Managers", managers.size());
    }

    /**
     * Notifier tous les Managers qu'un entretien a ete planifie.
     * Declenchee automatiquement lors de la planification d'un entretien.
     */
    @Transactional
    public void notifierManagerEntretienPlanifie(Entretien entretien) {
        Candidature candidature = entretien.getCandidature();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy a HH:mm");
        String dateFormatted = entretien.getDateEntretien().format(formatter);
        String candidatNom = candidature.getCandidat().getPrenom() + " " + candidature.getCandidat().getNom();
        String offreTitre = candidature.getOffre().getTitre();

        List<Utilisateur> managers = utilisateurRepository.findByTypeUtilisateur(TypeUtilisateur.manager);
        for (Utilisateur manager : managers) {
            Notification notification = Notification.builder()
                    .utilisateur(manager)
                    .titre("Entretien planifie")
                    .message("Un entretien a ete planifie le " + dateFormatted
                            + " pour " + candidatNom + " - poste \"" + offreTitre + "\".")
                    .type(TypeNotification.entretien_planifie_manager)
                    .build();
            notificationRepository.save(notification);
        }
        log.info("Notification entretien planifie envoyee a {} Managers", managers.size());
    }

    /**
     * Notifier tous les Managers d'une mise a jour sur une candidature.
     * Declenchee quand le RH modifie le statut d'une candidature sous responsabilite Manager.
     */
    @Transactional
    public void notifierManagerMiseAJourCandidature(Candidature candidature, String action) {
        String candidatNom = candidature.getCandidat().getPrenom() + " " + candidature.getCandidat().getNom();
        String offreTitre = candidature.getOffre().getTitre();

        List<Utilisateur> managers = utilisateurRepository.findByTypeUtilisateur(TypeUtilisateur.manager);
        for (Utilisateur manager : managers) {
            Notification notification = Notification.builder()
                    .utilisateur(manager)
                    .titre("Mise a jour candidature")
                    .message("La candidature de " + candidatNom + " pour le poste \""
                            + offreTitre + "\" a ete mise a jour par le RH. Action: " + action + ".")
                    .type(TypeNotification.mise_a_jour_candidature_manager)
                    .build();
            notificationRepository.save(notification);
        }
        log.info("Notification mise a jour candidature envoyee a {} Managers", managers.size());
    }

    // ================================================================
    //                  CONSULTATION DES NOTIFICATIONS
    // ================================================================

    /**
     * US-NOTIF-07: Consulter toutes les notifications d'un utilisateur.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMesNotifications(UUID utilisateurId) {
        List<Notification> notifications = notificationRepository
                .findByUtilisateurIdOrderByDateCreationDesc(utilisateurId);
        return notificationMapper.toResponseList(notifications);
    }

    /**
     * Consulter les notifications non lues d'un utilisateur.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMesNotificationsNonLues(UUID utilisateurId) {
        List<Notification> notifications = notificationRepository
                .findByUtilisateurIdAndLuFalseOrderByDateCreationDesc(utilisateurId);
        return notificationMapper.toResponseList(notifications);
    }

    /**
     * Compter les notifications non lues d'un utilisateur.
     */
    @Transactional(readOnly = true)
    public long countNonLues(UUID utilisateurId) {
        return notificationRepository.countByUtilisateurIdAndLuFalse(utilisateurId);
    }

    // ================================================================
    //                  MARQUER COMME LUE
    // ================================================================

    /**
     * US-NOTIF-08: Marquer une notification comme lue.
     */
    @Transactional
    public NotificationResponse marquerCommeLue(UUID notificationId, UUID utilisateurId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification non trouvee: " + notificationId));

        if (!notification.getUtilisateur().getId().equals(utilisateurId)) {
            throw new SecurityException("Acces non autorise a cette notification");
        }

        notification.setLu(true);
        Notification saved = notificationRepository.save(notification);
        log.info("Notification marquee comme lue: {}", notificationId);

        return notificationMapper.toResponse(saved);
    }

    /**
     * US-NOTIF-08: Marquer toutes les notifications d'un utilisateur comme lues.
     */
    @Transactional
    public int marquerToutesCommeLues(UUID utilisateurId) {
        List<Notification> nonLues = notificationRepository
                .findByUtilisateurIdAndLuFalseOrderByDateCreationDesc(utilisateurId);

        for (Notification notification : nonLues) {
            notification.setLu(true);
        }

        notificationRepository.saveAll(nonLues);
        log.info("Toutes les notifications marquees comme lues pour utilisateur: {}, count: {}",
                utilisateurId, nonLues.size());

        return nonLues.size();
    }
}
