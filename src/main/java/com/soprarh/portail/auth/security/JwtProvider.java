package com.soprarh.portail.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Composant responsable de la generation et validation des JWT.
 *
 * Algorithme : HS256 (HMAC-SHA256) — cle symetrique.
 * Librairie : JJWT 0.12.x (API fluente, pas d'usage de Strings deprecated).
 *
 * Claims du token :
 *   - sub  : email de l'utilisateur (identifiant unique)
 *   - iat  : date d'emission
 *   - exp  : date d'expiration
 *   - type : "access" ou "refresh"
 *   - jti  : identifiant unique du token (UUID) pour tracabilite
 *
 * Bonne pratique : ne jamais logger le token en clair.
 */
@Component
@Slf4j
public class JwtProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    /**
     * La cle secrete est lue depuis application.properties via variable d'environnement.
     * Elle est encodee en Base64 pour permettre des cles longues dans le fichier de config.
     * La conversion en SecretKey se fait une seule fois au demarrage (performance).
     */
    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs
    ) {
        // Keys.hmacShaKeyFor verifie que la cle est suffisamment longue pour HS256 (>= 256 bits)
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    /**
     * Genere un access token JWT pour l'utilisateur donne.
     *
     * @param userDetails UserDetails charge par Spring Security
     * @return JWT signe en chaine Base64url
     */
    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(userDetails.getUsername(), accessTokenExpirationMs,
                Map.of("type", "access"));
    }

    /**
     * Genere un refresh token JWT pour l'utilisateur donne.
     * Plus longue duree de vie, claims minimalistes.
     *
     * @param userDetails UserDetails charge par Spring Security
     * @return JWT signe en chaine Base64url
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails.getUsername(), refreshTokenExpirationMs,
                Map.of("type", "refresh"));
    }

    /**
     * Construit un token JWT avec les claims fournis.
     */
    private String buildToken(String subject, long expirationMs, Map<String, Object> extraClaims) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .id(UUID.randomUUID().toString())   // jti : identifiant unique du token
                .signWith(secretKey)                // HS256 par defaut avec SecretKey
                .compact();
    }

    /**
     * Extrait l'email (subject) du token.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrait le type du token ("access" ou "refresh").
     */
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    /**
     * Verifie si le token est valide pour l'utilisateur donne.
     * Conditions : signature valide + non expire + subject == username.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String email = extractEmail(token);
            return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException e) {
            log.warn("Token JWT invalide : {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verifie si le token est valide (signature + expiration) sans verifier l'utilisateur.
     * Utilise pour le refresh token.
     */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException e) {
            log.warn("Token JWT invalide : {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verifie si le token est un refresh token.
     */
    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractTokenType(token));
    }

    /**
     * Retourne la duree d'expiration de l'access token en millisecondes.
     * Utilisee dans AuthResponse.expiresIn.
     */
    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    // =========================================================
    // Methodes privees utilitaires
    // =========================================================

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        // Toute exception (signature invalide, expire, malformed) est propagee
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

