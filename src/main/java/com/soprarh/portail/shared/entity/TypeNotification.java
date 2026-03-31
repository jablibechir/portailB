package com.soprarh.portail.shared.entity;

/**
 * Enum pour le type de notification.
 * Valeurs exactes de la contrainte CHECK en base.
 */
public enum TypeNotification {
    // ==================== Notifications Candidat ====================
    activation_compte,
    candidature_soumise,
    candidature_rejetee_rh,
    candidature_envoyee_manager,
    candidature_rejetee_manager,
    entretien_planifie,

    // ==================== Notifications RH ====================
    nouvelle_candidature_recue,
    evaluation_manager_recue,
    candidature_retournee_manager,
    offre_expire_bientot,
    nouveau_compte_cree,

    // ==================== Notifications Manager ====================
    candidature_recue_pour_evaluation,
    nouvelle_candidature_offre_assignee,
    entretien_planifie_manager,
    mise_a_jour_candidature_manager
}
