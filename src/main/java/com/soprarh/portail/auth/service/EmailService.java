package com.soprarh.portail.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
        String link = baseUrl + "/activate?code=" + code;

        String htmlContent = buildActivationEmailHtml(nom, link);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Activation de votre compte - Portail de Recrutement Sopra RH");
            helper.setText(htmlContent, true);

            // Embed logo as inline image
            ClassPathResource logoResource = new ClassPathResource("static/logo-sopra-hr.png");
            if (logoResource.exists()) {
                helper.addInline("logoSopraHR", logoResource);
            }

            mailSender.send(mimeMessage);
            log.info("Email de verification envoye a : {}", toEmail);
        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de l'email de verification a : {}", toEmail, e);
            throw new RuntimeException("Impossible d'envoyer l'email de verification", e);
        }
    }

    private String buildActivationEmailHtml(String nom, String link) {
        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Plus Jakarta Sans', 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f8f6fc;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f8f6fc; padding: 40px 20px;">
                    <tr>
                        <td align="center">
                            <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 24px rgba(77, 28, 135, 0.10); overflow: hidden;">
                                
                                <!-- Header with Sopra HR gradient (purple → pink → yellow) -->
                                <tr>
                                    <td style="background: linear-gradient(135deg, #4D1C87 0%%, #7E00B5 40%%, #AE1F83 75%%, #FFCA23 100%%); padding: 40px 40px 30px 40px; text-align: center;">
                                        <img src="cid:logoSopraHR" alt="Sopra HR Software" style="max-width: 200px; height: auto; filter: brightness(0) invert(1);" />
                                        <p style="color: rgba(255,255,255,0.9); font-size: 14px; margin: 12px 0 0 0; letter-spacing: 1px; text-transform: uppercase;">Portail de Recrutement</p>
                                    </td>
                                </tr>
                                
                                <!-- Gradient accent bar -->
                                <tr>
                                    <td style="height: 4px; background: linear-gradient(90deg, #4D1C87 0%%, #7E00B5 35%%, #AE1F83 65%%, #FFCA23 100%%);"></td>
                                </tr>
                                
                                <!-- Body content -->
                                <tr>
                                    <td style="padding: 40px 40px 20px 40px;">
                                        <h1 style="color: #1D1D1B; font-size: 24px; margin: 0 0 8px 0; font-weight: 600;">Bienvenue sur notre plateforme !</h1>
                                        <div style="width: 60px; height: 3px; background: linear-gradient(90deg, #7E00B5, #AE1F83); margin: 0 0 24px 0; border-radius: 2px;"></div>
                                        
                                        <p style="color: #374151; font-size: 16px; line-height: 1.6; margin: 0 0 16px 0;">
                                            Bonjour <strong style="color: #4D1C87;">%s</strong>,
                                        </p>
                                        <p style="color: #374151; font-size: 16px; line-height: 1.6; margin: 0 0 16px 0;">
                                            Merci de vous \u00eatre inscrit sur le <strong style="color: #4D1C87;">Portail de Recrutement Sopra RH</strong>. Nous sommes ravis de vous compter parmi nos candidats.
                                        </p>
                                        <p style="color: #374151; font-size: 16px; line-height: 1.6; margin: 0 0 30px 0;">
                                            Pour finaliser votre inscription, veuillez activer votre compte en cliquant sur le bouton ci-dessous :
                                        </p>
                                        
                                        <!-- CTA Button -->
                                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                                            <tr>
                                                <td align="center" style="padding: 10px 0 30px 0;">
                                                    <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #4D1C87 0%%, #7E00B5 50%%, #AE1F83 100%%); color: #ffffff; text-decoration: none; padding: 16px 40px; border-radius: 8px; font-size: 16px; font-weight: 600; letter-spacing: 0.5px; box-shadow: 0 4px 14px rgba(126, 0, 181, 0.35);">
                                                        \u2713 Activer mon compte
                                                    </a>
                                                </td>
                                            </tr>
                                        </table>
                                        
                                        <!-- Info box -->
                                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f5f0fa; border-left: 4px solid #7E00B5; border-radius: 0 8px 8px 0;">
                                            <tr>
                                                <td style="padding: 16px 20px;">
                                                    <p style="color: #4D1C87; font-size: 14px; margin: 0; line-height: 1.5;">
                                                        \u23f0 <strong>Ce lien est valable 24 heures.</strong> Pass\u00e9 ce d\u00e9lai, vous devrez effectuer une nouvelle inscription.
                                                    </p>
                                                </td>
                                            </tr>
                                        </table>
                                        
                                        <!-- Fallback link -->
                                        <p style="color: #6b7280; font-size: 13px; line-height: 1.5; margin: 24px 0 0 0;">
                                            Si le bouton ne fonctionne pas, copiez et collez ce lien dans votre navigateur :<br/>
                                            <a href="%s" style="color: #7E00B5; word-break: break-all;">%s</a>
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- Divider -->
                                <tr>
                                    <td style="padding: 0 40px;">
                                        <hr style="border: none; border-top: 1px solid #e9e0f3; margin: 0;" />
                                    </td>
                                </tr>
                                
                                <!-- Footer -->
                                <tr>
                                    <td style="padding: 24px 40px 32px 40px; text-align: center;">
                                        <p style="color: #9ca3af; font-size: 12px; line-height: 1.5; margin: 0 0 8px 0;">
                                            Si vous n'\u00eates pas \u00e0 l'origine de cette inscription, vous pouvez ignorer cet email en toute s\u00e9curit\u00e9.
                                        </p>
                                        <p style="color: #9ca3af; font-size: 12px; line-height: 1.5; margin: 0 0 16px 0;">
                                            \u00a9 2024 Sopra HR Software \u2014 Tous droits r\u00e9serv\u00e9s
                                        </p>
                                        <table role="presentation" cellpadding="0" cellspacing="0" align="center">
                                            <tr>
                                                <td style="padding: 0 8px;">
                                                    <a href="#" style="color: #7E00B5; font-size: 12px; text-decoration: none;">Politique de confidentialit\u00e9</a>
                                                </td>
                                                <td style="color: #d1d5db; font-size: 12px;">|</td>
                                                <td style="padding: 0 8px;">
                                                    <a href="#" style="color: #7E00B5; font-size: 12px; text-decoration: none;">Conditions d'utilisation</a>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                                
                                <!-- Bottom gradient bar -->
                                <tr>
                                    <td style="height: 4px; background: linear-gradient(90deg, #4D1C87 0%%, #7E00B5 35%%, #AE1F83 65%%, #FFCA23 100%%);"></td>
                                </tr>
                                
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(nom, link, link, link);
    }
}

