INSERT INTO roles (name, created_at, updated_at, is_deleted)
VALUES ('ROLE_USER', NOW(), NOW(), FALSE),
       ('ROLE_OWNER', NOW(), NOW(), FALSE),
       ('ROLE_ADMIN', NOW(), NOW(), FALSE);
