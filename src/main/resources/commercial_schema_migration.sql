-- ==============================================================================
-- NQD-LMS Commercialization Domain Schema Migration (PostgreSQL)
-- Phase 1 Foundation: Products, Orders, Payments, Subscriptions, Entitlements
-- ==============================================================================

-- 1. PRODUCTS TABLE
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    product_type VARCHAR(32) NOT NULL,
    target_entity_id UUID,
    base_price NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    is_free BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    thumbnail_url TEXT,
    metadata TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by_user VARCHAR(255),
    updated_by VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_products_code ON products(code);
CREATE INDEX IF NOT EXISTS idx_products_type_status ON products(product_type, status);

-- 2. ORDERS TABLE
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_code VARCHAR(64) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id),
    total_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    discount_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    final_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(128) UNIQUE,
    coupon_code VARCHAR(64),
    notes TEXT,
    placed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    cancelled_at TIMESTAMP WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by_user VARCHAR(255),
    updated_by VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_orders_code ON orders(order_code);
CREATE INDEX IF NOT EXISTS idx_orders_user_status ON orders(user_id, status);
CREATE INDEX IF NOT EXISTS idx_orders_idempotency ON orders(idempotency_key);

-- 3. ORDER ITEMS TABLE
CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    product_title VARCHAR(255) NOT NULL,
    product_type VARCHAR(32) NOT NULL,
    unit_price NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    quantity INTEGER NOT NULL DEFAULT 1,
    subtotal NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    notes VARCHAR(500),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by_user VARCHAR(255),
    updated_by VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product ON order_items(product_id);

-- 4. PAYMENT TRANSACTIONS TABLE
CREATE TABLE IF NOT EXISTS payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_code VARCHAR(64) NOT NULL UNIQUE,
    order_id UUID NOT NULL REFERENCES orders(id),
    payment_method VARCHAR(32) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    gateway_reference VARCHAR(128),
    idempotency_key VARCHAR(128) UNIQUE,
    raw_callback_payload TEXT,
    failure_reason TEXT,
    processed_at TIMESTAMP WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by_user VARCHAR(255),
    updated_by VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_payment_txn_code ON payment_transactions(transaction_code);
CREATE INDEX IF NOT EXISTS idx_payment_txn_order ON payment_transactions(order_id);
CREATE INDEX IF NOT EXISTS idx_payment_txn_idempotency ON payment_transactions(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_payment_txn_gateway_ref ON payment_transactions(gateway_reference);

-- 5. MEMBERSHIP PLANS TABLE
CREATE TABLE IF NOT EXISTS membership_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    billing_cycle VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    ai_prompt_limit_per_month INTEGER NOT NULL DEFAULT 100,
    exam_creation_limit INTEGER NOT NULL DEFAULT 50,
    features TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by_user VARCHAR(255),
    updated_by VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_membership_plan_code ON membership_plans(plan_code);

-- 6. SUBSCRIPTIONS TABLE
CREATE TABLE IF NOT EXISTS subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    membership_plan_id UUID NOT NULL REFERENCES membership_plans(id),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    start_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    auto_renew BOOLEAN NOT NULL DEFAULT FALSE,
    cancelled_at TIMESTAMP WITHOUT TIME ZONE,
    source_order_id UUID REFERENCES orders(id),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by_user VARCHAR(255),
    updated_by VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_status ON subscriptions(user_id, status);
CREATE INDEX IF NOT EXISTS idx_subscriptions_end_date ON subscriptions(end_date);

-- 7. ENTITLEMENTS TABLE
CREATE TABLE IF NOT EXISTS entitlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    product_id UUID REFERENCES products(id),
    entitlement_type VARCHAR(32) NOT NULL,
    target_entity_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    valid_from TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    valid_until TIMESTAMP WITHOUT TIME ZONE,
    source_order_id UUID REFERENCES orders(id),
    source_subscription_id UUID REFERENCES subscriptions(id),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by_user VARCHAR(255),
    updated_by VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_entitlements_user_type_target ON entitlements(user_id, entitlement_type, target_entity_id);
CREATE INDEX IF NOT EXISTS idx_entitlements_user_status ON entitlements(user_id, status);
CREATE INDEX IF NOT EXISTS idx_entitlements_valid_until ON entitlements(valid_until);

-- 8. COUPONS TABLE
CREATE TABLE IF NOT EXISTS coupons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    discount_type VARCHAR(20) NOT NULL,
    discount_value NUMERIC(15, 2) NOT NULL,
    min_order_amount NUMERIC(15, 2) DEFAULT 0.00,
    max_discount_amount NUMERIC(15, 2),
    usage_limit INTEGER,
    used_count INTEGER NOT NULL DEFAULT 0,
    valid_from TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    valid_until TIMESTAMP WITHOUT TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by_user VARCHAR(255),
    updated_by VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_coupons_code ON coupons(code);
CREATE INDEX IF NOT EXISTS idx_coupons_active_valid ON coupons(is_active, valid_from, valid_until);
