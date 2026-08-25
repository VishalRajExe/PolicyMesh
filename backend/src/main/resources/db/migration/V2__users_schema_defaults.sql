-- =========================================================
-- PolicyMesh Migration V2: Ensure users table has enabled, name, and safe column defaults
-- Compatible with MySQL 8.x and H2 Test Database
-- =========================================================

-- In case users table was created from an older or external schema without defaults:
ALTER TABLE users ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS name VARCHAR(255) NULL;
