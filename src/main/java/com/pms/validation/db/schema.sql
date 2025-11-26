CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE stocks (
    stock_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cusip_id VARCHAR(255) NOT NULL,
    cusip_name VARCHAR(255) NOT,
    sector_name VARCHAR(255) NOT NULL
);


CREATE TABLE validated_outbox (
    trade_id UUID PRIMARY KEY,
    cusip_id VARCHAR(255),
    cusip_name VARCHAR(255),
    sector_name VARCHAR(255),
    side VARCHAR(10),
    price_per_stock DOUBLE,
    quantity BIGINT,
    timestamp VARCHAR(50),
    status VARCHAR(50)
);
