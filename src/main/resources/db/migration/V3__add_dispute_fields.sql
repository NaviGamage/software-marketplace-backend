-- Flyway Migration: V3__add_dispute_fields.sql
-- Description: Track dispute reason/resolution directly on order_items.

ALTER TABLE order_items
    ADD COLUMN dispute_reason TEXT,
    ADD COLUMN disputed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN resolution_note TEXT,
    ADD COLUMN resolved_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_order_items_disputed ON order_items(escrow_status) WHERE escrow_status = 'DISPUTED';