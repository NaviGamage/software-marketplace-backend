-- Flyway Migration: V4__add_review_aggregates.sql
-- Description: Store aggregated rating data on products for fast reads,
--              recalculated whenever a review is added.

ALTER TABLE products
    ADD COLUMN average_rating DECIMAL(3, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN review_count INT NOT NULL DEFAULT 0;