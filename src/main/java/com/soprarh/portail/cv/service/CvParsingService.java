package com.soprarh.portail.cv.service;

import com.soprarh.portail.cv.entity.*;
import com.soprarh.portail.cv.repository.DonneesCvRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Service qui appelle le microservice Python pour parser un CV
 * et sauvegarder les donnees extraites en base.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CvParsingService {

    private final WebClient aiWebClient;
    private final DonneesCvRepository donneesCvRepository;

    /**
     * Parse un CV PDF via le microservice Python et sauvegarde les donnees extraites.
     * Appele automatiquement apres l'upload d'un CV.
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public void parseAndSaveCvData(Cv cv, Path filePath) {
        try {
            log.info("Debut du parsing CV: cvId={}, fichier={}", cv.getId(), filePath);

            // 1. Appeler le microservice Python /api/extract-cv
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new FileSystemResource(filePath.toFile()));

            Map<String, Object> response = aiWebClient.post()
                    .uri("/api/extract-cv")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                log.warn("Echec du parsing CV: cvId={}", cv.getId());
                return;
            }

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null) {
                log.warn("Pas de donnees extraites pour CV: cvId={}", cv.getId());
                return;
            }

            // 2. Creer l'entite DonneesCv
            DonneesCv donneesCv = DonneesCv.builder()
                    .cv(cv)
                    .competences(getString(data, "competences"))
                    .experiences(getString(data, "experiences"))
                    .formations(getString(data, "formations"))
                    .langues(toJsonString(data.get("langues")))
                    .certifications(toJsonString(data.get("certifications")))
                    .softSkills(toJsonString(data.get("softSkills")))
                    .resume(getString(data, "resume"))
                    .build();

            // 3. Remplir les tables de detail

            // Competences
            List<Map<String, Object>> competencesDetail = (List<Map<String, Object>>) data.get("competencesDetail");
            if (competencesDetail != null) {
                for (Map<String, Object> c : competencesDetail) {
                    Competence comp = Competence.builder()
                            .donneesCv(donneesCv)
                            .nom(getString(c, "competence"))
                            .niveau(getString(c, "niveau"))
                            .build();
                    donneesCv.getCompetencesDetail().add(comp);
                }
            }

            // Experiences
            List<Map<String, Object>> experiencesDetail = (List<Map<String, Object>>) data.get("experiencesDetail");
            if (experiencesDetail != null) {
                for (Map<String, Object> e : experiencesDetail) {
                    Experience exp = Experience.builder()
                            .donneesCv(donneesCv)
                            .poste(getString(e, "poste"))
                            .entreprise(getString(e, "entreprise"))
                            .description(getString(e, "description"))
                            .build();
                    donneesCv.getExperiencesDetail().add(exp);
                }
            }

            // Formations
            List<Map<String, Object>> formationsDetail = (List<Map<String, Object>>) data.get("formationsDetail");
            if (formationsDetail != null) {
                for (Map<String, Object> f : formationsDetail) {
                    Formation form = Formation.builder()
                            .donneesCv(donneesCv)
                            .diplome(getString(f, "diplome"))
                            .etablissement(getString(f, "etablissement"))
                            .build();
                    donneesCv.getFormationsDetail().add(form);
                }
            }

            // Langues
            List<Map<String, Object>> langues = (List<Map<String, Object>>) data.get("langues");
            if (langues != null) {
                for (Map<String, Object> l : langues) {
                    Langue langue = Langue.builder()
                            .donneesCv(donneesCv)
                            .langue(getString(l, "langue"))
                            .niveau(getString(l, "niveau"))
                            .build();
                    donneesCv.getLanguesDetail().add(langue);
                }
            }

            // Certifications
            List<Map<String, Object>> certifications = (List<Map<String, Object>>) data.get("certifications");
            if (certifications != null) {
                for (Map<String, Object> c : certifications) {
                    Certification cert = Certification.builder()
                            .donneesCv(donneesCv)
                            .nom(getString(c, "certification"))
                            .build();
                    donneesCv.getCertificationsDetail().add(cert);
                }
            }

            // Soft Skills
            List<String> softSkills = (List<String>) data.get("softSkills");
            if (softSkills != null) {
                for (String s : softSkills) {
                    SoftSkill ss = SoftSkill.builder()
                            .donneesCv(donneesCv)
                            .nom(s)
                            .build();
                    donneesCv.getSoftSkillsDetail().add(ss);
                }
            }

            // 4. Sauvegarder tout (cascade)
            donneesCvRepository.save(donneesCv);
            cv.setDonneesCv(donneesCv);

            log.info("CV parse avec succes: cvId={}, competences={}, experiences={}, formations={}",
                    cv.getId(),
                    donneesCv.getCompetencesDetail().size(),
                    donneesCv.getExperiencesDetail().size(),
                    donneesCv.getFormationsDetail().size());

        } catch (Exception e) {
            // Ne pas bloquer la candidature si le parsing echoue
            log.error("Erreur lors du parsing du CV cvId={}: {}", cv.getId(), e.getMessage(), e);
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private String toJsonString(Object obj) {
        if (obj == null) return null;
        return obj.toString();
    }
}

