-- ================================================
-- Migration: Recommandations Manager
-- Ajoute les colonnes recommandation a offres_emploi
-- et la permission RECOMMEND_OFFERS pour le role MANAGER
-- ================================================

-- 1. Modifier le CHECK constraint sur statut pour accepter 'recommandee'
ALTER TABLE public.offres_emploi DROP CONSTRAINT IF EXISTS offres_emploi_statut_check;
ALTER TABLE public.offres_emploi ADD CONSTRAINT offres_emploi_statut_check
    CHECK (statut::text = ANY (ARRAY['brouillon', 'publiee', 'archivee', 'recommandee']::text[]));

-- 2. Ajouter les 3 nouvelles colonnes
ALTER TABLE public.offres_emploi ADD COLUMN IF NOT EXISTS recommandee_par uuid;
ALTER TABLE public.offres_emploi ADD COLUMN IF NOT EXISTS commentaire_manager text;
ALTER TABLE public.offres_emploi ADD COLUMN IF NOT EXISTS date_recommandation timestamp;

-- 3. Ajouter la FK vers utilisateurs
ALTER TABLE public.offres_emploi DROP CONSTRAINT IF EXISTS offres_emploi_recommandee_par_fkey;
ALTER TABLE public.offres_emploi ADD CONSTRAINT offres_emploi_recommandee_par_fkey
    FOREIGN KEY (recommandee_par) REFERENCES public.utilisateurs(id) ON DELETE SET NULL;

-- 4. Ajouter les nouveaux types de notification
ALTER TABLE public.notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
-- (Le type est gere par JPA/Hibernate, pas de CHECK constraint a modifier si le champ est varchar)

-- 5. Ajouter la permission RECOMMEND_OFFERS
INSERT INTO public.permissions (id, code, description)
VALUES (gen_random_uuid(), 'RECOMMEND_OFFERS', 'Recommander un poste au RH')
ON CONFLICT (code) DO NOTHING;

-- 6. Associer RECOMMEND_OFFERS au role MANAGER
INSERT INTO public.role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM public.roles WHERE nom = 'MANAGER'),
    (SELECT id FROM public.permissions WHERE code = 'RECOMMEND_OFFERS')
WHERE NOT EXISTS (
    SELECT 1 FROM public.role_permissions rp
    JOIN public.roles r ON rp.role_id = r.id
    JOIN public.permissions p ON rp.permission_id = p.id
    WHERE r.nom = 'MANAGER' AND p.code = 'RECOMMEND_OFFERS'
);

-- Verification
SELECT r.nom as role, p.code as permission
FROM public.roles r
JOIN public.role_permissions rp ON r.id = rp.role_id
JOIN public.permissions p ON rp.permission_id = p.id
WHERE p.code = 'RECOMMEND_OFFERS'
ORDER BY r.nom;
