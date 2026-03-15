-- ================================================
-- DIAGNOSTIC : Vérifier les associations utilisateur-rôle-permission
-- Exécute ces requêtes dans pgAdmin ou psql pour comprendre le problème du 403
-- ================================================

-- 1. Vérifier si la table utilisateur_roles contient des données
SELECT 'Table utilisateur_roles' as table_name, COUNT(*) as nb_lignes
FROM public.utilisateur_roles;

-- 2. Voir tous les utilisateurs et leurs rôles (ou NULL si pas de rôle)
SELECT
    u.id as utilisateur_id,
    u.nom,
    u.email,
    u.type_utilisateur,
    u.etat,
    r.nom as role_assigne
FROM public.utilisateurs u
LEFT JOIN public.utilisateur_roles ur ON u.id = ur.utilisateur_id
LEFT JOIN public.roles r ON ur.role_id = r.id
ORDER BY u.nom;

-- 3. Voir les permissions de chaque rôle
SELECT
    r.nom as role,
    p.code as permission,
    p.description
FROM public.roles r
JOIN public.role_permissions rp ON r.id = rp.role_id
JOIN public.permissions p ON rp.permission_id = p.id
ORDER BY r.nom, p.code;

-- 4. Vérifier spécifiquement l'utilisateur RH Pierre Durand
SELECT
    u.nom,
    u.email,
    u.type_utilisateur,
    r.nom as role,
    p.code as permission
FROM public.utilisateurs u
LEFT JOIN public.utilisateur_roles ur ON u.id = ur.utilisateur_id
LEFT JOIN public.roles r ON ur.role_id = r.id
LEFT JOIN public.role_permissions rp ON r.id = rp.role_id
LEFT JOIN public.permissions p ON rp.permission_id = p.id
WHERE u.email = 'pierre.durand@entreprise.com';

-- 5. Résumé : qui a la permission MANAGE_OFFERS ?
SELECT
    u.nom,
    u.email,
    r.nom as role,
    p.code as permission
FROM public.utilisateurs u
JOIN public.utilisateur_roles ur ON u.id = ur.utilisateur_id
JOIN public.roles r ON ur.role_id = r.id
JOIN public.role_permissions rp ON r.id = rp.role_id
JOIN public.permissions p ON rp.permission_id = p.id
WHERE p.code = 'MANAGE_OFFERS';

