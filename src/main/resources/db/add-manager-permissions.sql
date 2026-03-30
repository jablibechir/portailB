-- ================================================
-- Script pour ajouter les nouvelles permissions
-- Necessaires pour le workflow RH -> Manager
-- ================================================

-- 1. Ajouter la permission VALIDATE_CANDIDATES si elle n'existe pas
INSERT INTO public.permissions (id, code, description)
VALUES (gen_random_uuid(), 'VALIDATE_CANDIDATES', 'Valider ou rejeter les candidatures transmises')
ON CONFLICT (code) DO NOTHING;

-- 2. Ajouter la permission TRANSMIT_TO_MANAGER si elle n'existe pas
INSERT INTO public.permissions (id, code, description)
VALUES (gen_random_uuid(), 'TRANSMIT_TO_MANAGER', 'Transmettre une candidature au Manager')
ON CONFLICT (code) DO NOTHING;

-- 3. Associer VALIDATE_CANDIDATES au role MANAGER
INSERT INTO public.role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM public.roles WHERE nom = 'MANAGER'),
    (SELECT id FROM public.permissions WHERE code = 'VALIDATE_CANDIDATES')
WHERE NOT EXISTS (
    SELECT 1 FROM public.role_permissions rp
    JOIN public.roles r ON rp.role_id = r.id
    JOIN public.permissions p ON rp.permission_id = p.id
    WHERE r.nom = 'MANAGER' AND p.code = 'VALIDATE_CANDIDATES'
);

-- 4. Associer TRANSMIT_TO_MANAGER au role RH
INSERT INTO public.role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM public.roles WHERE nom = 'RH'),
    (SELECT id FROM public.permissions WHERE code = 'TRANSMIT_TO_MANAGER')
WHERE NOT EXISTS (
    SELECT 1 FROM public.role_permissions rp
    JOIN public.roles r ON rp.role_id = r.id
    JOIN public.permissions p ON rp.permission_id = p.id
    WHERE r.nom = 'RH' AND p.code = 'TRANSMIT_TO_MANAGER'
);

-- Verification : afficher les permissions par role
SELECT r.nom as role, p.code as permission, p.description
FROM public.roles r
JOIN public.role_permissions rp ON r.id = rp.role_id
JOIN public.permissions p ON rp.permission_id = p.id
ORDER BY r.nom, p.code;

