
-- 1. invalid_trades
CREATE TABLE validation_invalid_trades (
    invalid_trade_outbox_id BIGSERIAL PRIMARY KEY,

    event_id UUID,
    trade_id UUID,
    portfolio_id UUID,

    symbol VARCHAR(255),

    side VARCHAR(50),

    price_per_stock NUMERIC(19, 4),
    quantity BIGINT,

    trade_timestamp TIMESTAMP,

    sent_status VARCHAR(255),
    validation_status VARCHAR(255),
    validation_errors TEXT,

    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 2. processed_messages
CREATE TABLE validation_processed_messages (
    id BIGSERIAL PRIMARY KEY,

    trade_id UUID NOT NULL,
    consumer_group VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP NOT NULL,

    CONSTRAINT uk_trade_id_consumer_group
        UNIQUE (trade_id, consumer_group)
);

-- 3. stocks
CREATE TABLE pms_stocks (
    stock_id BIGSERIAL PRIMARY KEY,

    symbol VARCHAR(255) NOT NULL,
    sector_name VARCHAR(100) NOT NULL,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- 4. validation_outbox
CREATE TABLE validation_outbox (
    validation_outbox_id BIGSERIAL PRIMARY KEY,

    event_id UUID NOT NULL,
    trade_id UUID NOT NULL,
    portfolio_id UUID NOT NULL,

    symbol VARCHAR(255) NOT NULL,
    side VARCHAR(50) NOT NULL,

    price_per_stock NUMERIC(19, 4) NOT NULL,
    quantity BIGINT NOT NULL,

    trade_timestamp TIMESTAMP NOT NULL,

    sent_status VARCHAR(255) NOT NULL,
    validation_status VARCHAR(255) NOT NULL,
    validation_errors TEXT,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- 5. validation_dlq_entry
CREATE TABLE validation_dlq_entry (
    dlq_entry_id BIGSERIAL PRIMARY KEY,

    payload BYTEA NOT NULL,

    error_detail TEXT,

    created_at TIMESTAMP NOT NULL
);
