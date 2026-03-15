package com.soprarh.portail.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Mappe exactement la table "roles" et la table de jonction "role_permissions".
 *
 * CREATE TABLE public.roles (
 *   id  uuid DEFAULT gen_random_uuid() NOT NULL,
 *   nom varchar(50) NOT NULL UNIQUE
 * );
 *
 * CREATE TABLE public.role_permissions (
 *   role_id       uuid NOT NULL,  -> FK roles(id)   CASCADE
 *   permission_id uuid NOT NULL   -> FK permissions(id) CASCADE
 *   PK (role_id, permission_id)
 * );
 */
@Entity
@Table(name = "roles", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * Nom du role. Ex: "CANDIDAT", "MANAGER", "RH"
     * Unique en base, NOT NULL.
     */
    @Column(name = "nom", length = 50, nullable = false, unique = true)
    private String nom;

    /**
     * Relation ManyToMany vers Permission.
     * Proprietaire de la relation : c'est Role qui porte le @JoinTable
     * pour mapper exactement la table "role_permissions".
     *
     * joinColumns        = colonne de Role dans la table de jonction
     * inverseJoinColumns = colonne de Permission dans la table de jonction
     */
    @ManyToMany(fetch = FetchType.EAGER,
                cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "role_permissions",
        schema = "public",
        joinColumns        = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    /**
     * Cote inverse de la relation ManyToMany Utilisateur <-> Role.
     * mappedBy designe le champ "roles" dans Utilisateur qui porte le @JoinTable.
     */
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Utilisateur> utilisateurs = new HashSet<>();
}

