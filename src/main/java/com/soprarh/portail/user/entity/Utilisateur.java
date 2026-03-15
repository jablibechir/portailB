package com.soprarh.portail.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mappe exactement la table "utilisateurs" et la table de jonction "utilisateur_roles".
 *
 * CREATE TABLE public.utilisateurs (
 *   id               uuid DEFAULT gen_random_uuid() NOT NULL,
 *   nom              varchar(100) NOT NULL,
 *   email            varchar(255) NOT NULL UNIQUE,
 *   mot_de_passe     varchar(255) NOT NULL,
 *   etat             varchar(20)  DEFAULT 'actif'  CHECK (actif | inactif | suspendu),
 *   type_utilisateur varchar(20)  NOT NULL          CHECK (candidat | manager | rh),
 *   date_creation    timestamp    DEFAULT CURRENT_TIMESTAMP
 * );
 *
 * CREATE TABLE public.utilisateur_roles (
 *   utilisateur_id uuid NOT NULL,  -> FK utilisateurs(id) CASCADE
 *   role_id        uuid NOT NULL   -> FK roles(id)        CASCADE
 *   PK (utilisateur_id, role_id)
 * );
 *
 * Implements UserDetails pour l'integration Spring Security.
 * Les autorisations sont derivees des permissions des roles (pas des noms de roles).
 */
@Entity
@Table(name = "utilisateurs", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Utilisateur implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * Nom de famille de l'utilisateur.
     * Colonne : nom varchar(100) NOT NULL
     */
    @Column(name = "nom", length = 100, nullable = false)
    private String nom;

    /**
     * Prenom de l'utilisateur.
     * Colonne : prenom varchar(100)
     */
    @Column(name = "prenom", length = 100)
    private String prenom;

    /**
     * Email unique — utilise comme username dans Spring Security.
     * Colonne : email varchar(255) NOT NULL UNIQUE
     */
    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    /**
     * Mot de passe hache (BCrypt).
     * Colonne : mot_de_passe varchar(255) NOT NULL
     * IMPORTANT : jamais expose dans un DTO de reponse.
     */
    @Column(name = "mot_de_passe", length = 255, nullable = false)
    private String motDePasse;

    /**
     * Etat du compte.
     * Colonne : etat varchar(20) DEFAULT 'actif'
     * Mappe via @Enumerated(STRING) pour stocker la valeur textuelle exacte.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "etat", length = 20, nullable = false)
    @Builder.Default
    private EtatUtilisateur etat = EtatUtilisateur.actif;

    /**
     * Type structurel de l'utilisateur dans l'application.
     * Colonne : type_utilisateur varchar(20) NOT NULL
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_utilisateur", length = 20, nullable = false)
    private TypeUtilisateur typeUtilisateur;

    /**
     * Date de creation du compte. Non modifiable apres insertion.
     * Colonne : date_creation timestamp DEFAULT CURRENT_TIMESTAMP
     */
    @Column(name = "date_creation", updatable = false)
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    /**
     * Code unique pour la verification email.
     * Colonne : code_verification varchar(100)
     */
    @Column(name = "code_verification", length = 100)
    private String codeVerification;

    /**
     * Date d'expiration du code de verification.
     * Colonne : code_expiration timestamp
     */
    @Column(name = "code_expiration")
    private LocalDateTime codeExpiration;

    /**
     * Relation ManyToMany vers Role.
     * Proprietaire de la relation : Utilisateur porte le @JoinTable
     * pour mapper exactement la table "utilisateur_roles".
     *
     * EAGER : les roles sont toujours charges avec l'utilisateur
     * car Spring Security en a besoin immediatement pour getAuthorities().
     */
    @ManyToMany(fetch = FetchType.EAGER,
                cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "utilisateur_roles",
        schema = "public",
        joinColumns        = @JoinColumn(name = "utilisateur_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    // =========================================================
    // UserDetails — Spring Security
    // =========================================================

    /**
     * Construit les GrantedAuthority a partir des PERMISSIONS des roles.
     * Chaine : roles -> permissions -> code -> SimpleGrantedAuthority
     * Ex: "VIEW_OFFERS", "MANAGE_USERS"
     *
     * Pourquoi les permissions et pas les noms de roles ?
     * Les permissions sont granulaires et permettent un RBAC fin.
     * Spring Security @PreAuthorize("hasAuthority('MANAGE_USERS')") fonctionne directement.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> new SimpleGrantedAuthority(permission.getCode()))
                .collect(Collectors.toSet());
    }

    /**
     * Spring Security utilise getPassword() — delegue vers motDePasse.
     */
    @Override
    public String getPassword() {
        return motDePasse;
    }

    /**
     * Spring Security utilise getUsername() — on utilise l'email comme identifiant unique.
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Compte non expire — pas de logique d'expiration prevue dans le schema.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Compte non verrouille si etat != suspendu.
     */
    @Override
    public boolean isAccountNonLocked() {
        return etat != EtatUtilisateur.suspendu;
    }

    /**
     * Credentials non expires — pas de logique prevue dans le schema.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Compte actif uniquement si etat == actif.
     */
    @Override
    public boolean isEnabled() {
        return etat == EtatUtilisateur.actif;
    }
}

