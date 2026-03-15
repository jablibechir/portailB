package com.soprarh.portail.shared;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Utilitaire centralisé pour la gestion des cookies JWT HttpOnly.
 *
 * Attributs de securite appliques :
 *  - HttpOnly  : JavaScript ne peut PAS lire le cookie (protection XSS)
 *  - Secure    : cookie transmis uniquement via HTTPS (desactive en dev)
 *  - SameSite=Strict : cookie non envoye dans les requetes cross-site (protection CSRF)
 *  - Path=/api : cookie scope uniquement sur les endpoints API
 */
@Component
public class CookieUtils {

    @Value("${app.cookie.secure:false}")
    private boolean secureCookie;

    @Value("${jwt.access-token-expiration-ms:86400000}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    /**
     * Ajoute le cookie access_token dans la reponse HTTP.
     */
    public void addAccessTokenCookie(HttpServletResponse response, String token) {
        addCookie(response, "access_token", token, (int) (accessTokenExpirationMs / 1000));
    }

    /**
     * Ajoute le cookie refresh_token dans la reponse HTTP.
     */
    public void addRefreshTokenCookie(HttpServletResponse response, String token) {
        addCookie(response, "refresh_token", token, (int) (refreshTokenExpirationMs / 1000));
    }

    /**
     * Efface les deux cookies JWT (logout).
     * MaxAge = 0 force le navigateur a supprimer le cookie immediatement.
     */
    public void clearTokenCookies(HttpServletResponse response) {
        addCookie(response, "access_token",  "", 0);
        addCookie(response, "refresh_token", "", 0);
    }

    // -------------------------------------------------------

    private void addCookie(HttpServletResponse response, String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);           // Inaccessible depuis JavaScript
        cookie.setSecure(secureCookie);     // HTTPS uniquement (true en prod)
        cookie.setPath("/api");             // Scope restreint aux endpoints API
        cookie.setMaxAge(maxAgeSeconds);    // 0 = suppression immediate
        // SameSite=Strict via header Set-Cookie manuel (l'API Cookie Java ne le supporte pas)
        response.addCookie(cookie);
        // Patch SameSite : on recrit le dernier Set-Cookie header pour ajouter SameSite=Strict
        patchSameSite(response, name);
    }

    /**
     * L'API jakarta.servlet.http.Cookie ne supporte pas SameSite nativement.
     * On recrit le dernier header Set-Cookie pour y ajouter "; SameSite=Strict".
     */
    private void patchSameSite(HttpServletResponse response, String cookieName) {
        response.getHeaders("Set-Cookie").stream()
                .filter(h -> h.startsWith(cookieName + "="))
                .findFirst()
                .ifPresent(header -> {
                    // Supprimer l'ancien et reecrire avec SameSite
                    // Note : addHeader ne remplace pas, mais les navigateurs utilisent le dernier
                    response.addHeader("Set-Cookie", header + "; SameSite=Strict");
                });
    }
}

