package com.soprarh.portail.shared.entity;

import com.soprarh.portail.user.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entite mappant exactement la table "notifications".
 *
 * CREATE TABLE public.notifications (
 *   id              uuid DEFAULT gen_random_uuid() NOT NULL,
 *   utilisateur_id  uuid -> FK utilisateurs(id),
 *   titre           varchar(255) NOT NULL,
 *   message         text NOT NULL,
 *   type            varchar(50),
 *   lu              boolean DEFAULT false,
 *   date_creation   timestamp DEFAULT CURRENT_TIMESTAMP,
 *   CONSTRAINT notifications_type_check CHECK (type IN (
 *       'activation_compte','candidature_soumise',
 *       'candidature_rejetee_rh','candidature_envoyee_manager',
 *       'candidature_rejetee_manager','entretien_planifie'
 *   ))
 * );
 */
@Entity
@Table(name = "notifications", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * Utilisateur destinataire de la notification.
     * FK: utilisateur_id -> utilisateurs(id)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    /**
     * Titre de la notification.
     * Colonne: titre varchar(255) NOT NULL
     */
    @Column(name = "titre", length = 255, nullable = false)
    private String titre;

    /**
     * Contenu du message.
     * Colonne: message text NOT NULL
     */
    @Column(name = "message", columnDefinition = "text", nullable = false)
    private String message;

    /**
     * Type de notification.
     * Colonne: type varchar(50)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50)
    private TypeNotification type;

    /**
     * Indique si la notification a ete lue.
     * Colonne: lu boolean DEFAULT false
     */
    @Column(name = "lu")
    @Builder.Default
    private Boolean lu = false;

    /**
     * Date de creation de la notification.
     * Colonne: date_creation timestamp DEFAULT CURRENT_TIMESTAMP
     */
    @Column(name = "date_creation", updatable = false)
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}

