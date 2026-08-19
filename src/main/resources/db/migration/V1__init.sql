-- Flyway Migration: V1__init.sql
-- Description: Initial schema for Digital Software Marketplace

-- ============================================================
-- 1. Enums / Custom Types
-- ============================================================
CREATE TYPE user_role AS ENUM ('ADMIN', 'VENDOR', 'BUYER');
CREATE TYPE user_status AS ENUM ('PENDING', 'ACTIVE', 'SUSPENDED');
CREATE TYPE product_status AS ENUM ('DRAFT', 'SCANNING', 'PENDING_REVIEW', 'APPROVED', 'REJECTED', 'SUSPENDED');
CREATE TYPE license_type AS ENUM ('REGULAR', 'EXTENDED');
CREATE TYPE order_status AS ENUM ('PENDING', 'PAID', 'FAILED', 'REFUNDED');
CREATE TYPE escrow_status AS ENUM ('HOLDING', 'COMPLETED', 'DISPUTED', 'REFUNDED');
CREATE TYPE payout_status AS ENUM ('PENDING', 'PROCESSED', 'REJECTED');

-- ============================================================
-- Trigger function: auto-update `updated_at` on row UPDATE
-- ============================================================
CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- 2. Users Table
-- ============================================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    role user_role NOT NULL DEFAULT 'BUYER',
    status user_status NOT NULL DEFAULT 'ACTIVE',
    stripe_connected_account_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER set_timestamp_users
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ============================================================
-- 3. Categories Table
-- ============================================================
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 4. Products Table
-- ============================================================
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    vendor_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    demo_url VARCHAR(500),
    price_regular DECIMAL(10, 2) NOT NULL,
    price_extended DECIMAL(10, 2) NOT NULL,
    current_version VARCHAR(20) DEFAULT '1.0.0',
    file_key VARCHAR(500),          -- S3 Key
    file_sha256 VARCHAR(64),        -- SHA-256 hash for duplicate detection
    status product_status NOT NULL DEFAULT 'DRAFT',
    rejection_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER set_timestamp_products
BEFORE UPDATE ON products
FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ============================================================
-- 5. Product Versions Table
-- ============================================================
CREATE TABLE product_versions (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    version_number VARCHAR(20) NOT NULL,
    changelog TEXT,
    file_key VARCHAR(500) NOT NULL,
    file_sha256 VARCHAR(64),
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_product_version UNIQUE (product_id, version_number)
);

-- Only one "current" version per product
CREATE UNIQUE INDEX idx_product_versions_current
    ON product_versions(product_id)
    WHERE is_current = TRUE;

-- ============================================================
-- 6. Orders Table (cart-level checkout record; can span vendors)
-- ============================================================
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    buyer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    total_amount DECIMAL(10, 2) NOT NULL,
    status order_status NOT NULL DEFAULT 'PENDING',
    stripe_payment_intent_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER set_timestamp_orders
BEFORE UPDATE ON orders
FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ============================================================
-- 7. Order Items Table
--    Escrow lives HERE (per line item), not on the order,
--    because a single order/cart can contain products from
--    multiple vendors, each with an independent payout timeline.
-- ============================================================
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    vendor_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    license license_type NOT NULL DEFAULT 'REGULAR',
    price DECIMAL(10, 2) NOT NULL,
    platform_fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    vendor_earnings DECIMAL(10, 2) NOT NULL,
    escrow_status escrow_status NOT NULL DEFAULT 'HOLDING',
    escrow_release_date TIMESTAMP WITH TIME ZONE, -- e.g. 14 days after purchase
    is_downloaded BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER set_timestamp_order_items
BEFORE UPDATE ON order_items
FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ============================================================
-- 8. Downloads Table (Token-based Secure Downloads)
-- ============================================================
CREATE TABLE downloads (
    id BIGSERIAL PRIMARY KEY,
    order_item_id BIGINT NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    download_token VARCHAR(255) NOT NULL UNIQUE,
    token_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    download_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 9. Reviews Table
-- ============================================================
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    buyer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_buyer_product_review UNIQUE (product_id, buyer_id)
);

-- ============================================================
-- 10. Payouts Table
-- ============================================================
CREATE TABLE payouts (
    id BIGSERIAL PRIMARY KEY,
    vendor_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    amount DECIMAL(10, 2) NOT NULL,
    status payout_status NOT NULL DEFAULT 'PENDING',
    stripe_transfer_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER set_timestamp_payouts
BEFORE UPDATE ON payouts
FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- ============================================================
-- Indexes for performance optimization
-- ============================================================

-- Products
CREATE INDEX idx_products_vendor ON products(vendor_id);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_status ON products(status);
CREATE INDEX idx_products_file_hash ON products(file_sha256);

-- Product versions
CREATE INDEX idx_product_versions_product ON product_versions(product_id);

-- Orders / order items
CREATE INDEX idx_orders_buyer ON orders(buyer_id);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_vendor ON order_items(vendor_id);
CREATE INDEX idx_order_items_escrow_status ON order_items(escrow_status);

-- Downloads
CREATE INDEX idx_downloads_token ON downloads(download_token);
CREATE INDEX idx_downloads_token_expiry ON downloads(token_expires_at);

-- Reviews
CREATE INDEX idx_reviews_product ON reviews(product_id);

-- Payouts
CREATE INDEX idx_payouts_vendor ON payouts(vendor_id);
