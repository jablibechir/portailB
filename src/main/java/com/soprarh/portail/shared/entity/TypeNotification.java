package com.soprarh.portail.shared.entity;

/**
 * Enum pour le type de notification.
 * Valeurs exactes de la contrainte CHECK en base:
 * CHECK (type IN (
 *     'activation_compte','candidature_soumise',
 *     'candidature_rejetee_rh','candidature_envoyee_manager',
 *     'candidature_rejetee_manager','entretien_planifie'
 * ))
 */
public enum TypeNotification {
    activation_compte,
    candidature_soumise,
    candidature_rejetee_rh,
    candidature_envoyee_manager,
    candidature_rejetee_manager,
    entretien_planifie
}

