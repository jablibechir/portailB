-- Migration: Add langues_requises, certifications_requises, soft_skills_requis to offres_emploi
-- Date: 2026-05-02

ALTER TABLE public.offres_emploi
    ADD COLUMN IF NOT EXISTS langues_requises TEXT,
    ADD COLUMN IF NOT EXISTS certifications_requises TEXT,
    ADD COLUMN IF NOT EXISTS soft_skills_requis TEXT;
