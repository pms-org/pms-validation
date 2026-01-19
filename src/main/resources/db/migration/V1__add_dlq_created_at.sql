-- Migration: add created_at column to dlq_entry table
-- Run this manually if you don't have an automated migration tool (Flyway/Liquibase).

ALTER TABLE IF EXISTS dlq_entry
  ADD COLUMN IF NOT EXISTS created_at timestamp without time zone NOT NULL DEFAULT now();

-- Ensure payload and error_detail columns exist (no-op if already present)
ALTER TABLE IF EXISTS dlq_entry
  ADD COLUMN IF NOT EXISTS payload bytea;

ALTER TABLE IF EXISTS dlq_entry
  ADD COLUMN IF NOT EXISTS error_detail text;
