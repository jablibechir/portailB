package com.soprarh.portail.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service responsable de l'envoi des emails transactionnels.
 *
 * Utilise JavaMailSender (Spring Boot Starter Mail) avec la config SMTP
 * definie dans application.properties (spring.mail.*).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    /**
     * Envoie le lien d'activation a l'adresse email de l'utilisateur.
     *
     * @param toEmail  adresse de destination
     * @param nom      prenom ou nom pour personnaliser le message
     * @param code     code unique de verification (UUID)
     * @param baseUrl  URL de base de l'application (ex: http://localhost:8080)
     */
    public void sendVerificationEmail(String toEmail, String nom, String code, String baseUrl) {
        String link = baseUrl + "/api/auth/verify?code=" + code;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Activation de votre compte - Portail de Recrutement");
        message.setText(
                "Bonjour " + nom + ",\n\n" +
                "Merci de vous etre inscrit sur le Portail de Recrutement Sopra RH.\n\n" +
                "Cliquez sur le lien ci-dessous pour activer votre compte (valable 24 heures) :\n\n" +
                link + "\n\n" +
                "Si vous n'etes pas a l'origine de cette inscription, ignorez cet email.\n\n" +
                "Cordialement,\nL'equipe Sopra RH"
        );

        mailSender.send(message);
        log.info("Email de verification envoye a : {}", toEmail);
    }
}

