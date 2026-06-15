-- V012: Add new candidature statuses for interview workflow
-- entretien_confirme: interview has been confirmed
-- entretien_termine: interview has been completed

-- Drop old constraint
ALTER TABLE public.candidatures DROP CONSTRAINT IF EXISTS candidatures_statut_check;

-- Add new constraint with additional statuses
ALTER TABLE public.candidatures ADD CONSTRAINT candidatures_statut_check 
CHECK (((statut)::text = ANY ((ARRAY[
    'soumise'::character varying, 
    'en_evaluation_rh'::character varying, 
    'envoyee_manager'::character varying, 
    'acceptee_manager'::character varying, 
    'rejetee_rh'::character varying, 
    'rejetee_manager'::character varying, 
    'entretien_planifie'::character varying,
    'entretien_confirme'::character varying,
    'entretien_termine'::character varying
])::text[])));
