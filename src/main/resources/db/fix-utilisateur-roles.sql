-- ================================================
-- Script pour corriger les associations utilisateur-role
-- Le problème : la table utilisateur_roles est vide,
-- donc les utilisateurs n'ont aucune permission.
-- ================================================

-- Associer Jean Dupont (candidat) au rôle CANDIDAT
INSERT INTO public.utilisateur_roles (utilisateur_id, role_id)
VALUES ('b9cd7c23-97e6-4847-993f-326aaf7f22e6', 'e272aec0-9fb2-4ad1-8471-b83219b519c1')
ON CONFLICT DO NOTHING;

-- Associer Marie Martin (manager) au rôle MANAGER
INSERT INTO public.utilisateur_roles (utilisateur_id, role_id)
VALUES ('3dd91610-db78-4ba0-8417-2db402799432', '2ecd49d9-b290-4ca4-86fe-64d43e013a9d')
ON CONFLICT DO NOTHING;

-- Associer Pierre Durand (rh) au rôle RH
INSERT INTO public.utilisateur_roles (utilisateur_id, role_id)
VALUES ('45c50d54-4dfa-4091-a133-8828bd2666bb', '163c6be0-94b2-48d2-8d38-28159360cf19')
ON CONFLICT DO NOTHING;

-- Vérification : afficher les associations
SELECT u.nom, u.email, u.type_utilisateur, r.nom as role_nom
FROM public.utilisateurs u
LEFT JOIN public.utilisateur_roles ur ON u.id = ur.utilisateur_id
LEFT JOIN public.roles r ON ur.role_id = r.id;

-- Vérification : permissions du rôle RH
SELECT r.nom as role, p.code as permission
FROM public.roles r
JOIN public.role_permissions rp ON r.id = rp.role_id
JOIN public.permissions p ON rp.permission_id = p.id
WHERE r.nom = 'RH';

