package com.soprarh.portail.user.entity;

/**
 * Etat du compte utilisateur.
 * Mappe exactement le CHECK constraint de la colonne "etat" dans la table utilisateurs.
 * Valeurs SQL : 'actif', 'inactif', 'suspendu'
 */
public enum EtatUtilisateur {
    actif,
    inactif,
    suspendu
}

