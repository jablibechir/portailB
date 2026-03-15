package com.soprarh.portail.auth.service;

import com.soprarh.portail.user.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation de UserDetailsService pour Spring Security.
 *
 * Spring Security appelle loadUserByUsername() lors de chaque authentification
 * pour charger l'utilisateur depuis la DB et verifier les credentials.
 *
 * Notre entite Utilisateur implementant UserDetails,
 * on retourne directement l'entite.
 *
 * @Transactional(readOnly = true) : optimisation — lecture seule, pas de flush Hibernate.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return utilisateurRepository.findByEmailWithRolesAndPermissions(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur non trouve avec l'email : " + email
                ));
    }
}

