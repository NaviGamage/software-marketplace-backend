-- Flyway Migration: V2__add_payout_linking.sql
-- Description: Link order_items to payouts to prevent double-payout,
--              and add rejection tracking to payouts.

ALTER TABLE order_items
    ADD COLUMN payout_id BIGINT REFERENCES payouts(id) ON DELETE SET NULL;

CREATE INDEX idx_order_items_payout ON order_items(payout_id);

ALTER TABLE payouts
    ADD COLUMN rejection_reason TEXT,
    ADD COLUMN processed_at TIMESTAMP WITH TIME ZONE;