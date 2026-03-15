package com.soprarh.portail.user.entity;

/**
 * Type structurel de l'utilisateur dans l'application.
 * Mappe exactement le CHECK constraint de la colonne "type_utilisateur" dans la table utilisateurs.
 * Valeurs SQL : 'candidat', 'manager', 'rh'
 */
public enum TypeUtilisateur {
    candidat,
    manager,
    rh
}

