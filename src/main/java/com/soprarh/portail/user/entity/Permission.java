package com.soprarh.portail.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Mappe exactement la table "permissions".
 *
 * CREATE TABLE public.permissions (
 *   id          uuid DEFAULT gen_random_uuid() NOT NULL,
 *   code        varchar(50) NOT NULL UNIQUE,
 *   description text
 * );
 */
@Entity
@Table(name = "permissions", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * Code unique de la permission. Ex: "VIEW_OFFERS", "MANAGE_USERS"
     * Utilise comme GrantedAuthority dans Spring Security.
     */
    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    /**
     * Description lisible de la permission. Nullable en base.
     */
    @Column(name = "description", columnDefinition = "text")
    private String description;

    /**
     * Cote inverse de la relation ManyToMany Role <-> Permission.
     * mappedBy designe le champ "permissions" dans Role qui porte le @JoinTable.
     * FetchType.LAZY : chargement uniquement si acces explicite.
     */
    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
}

