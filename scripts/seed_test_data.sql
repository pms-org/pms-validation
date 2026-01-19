-- Seed data for local testing: portfolios and stocks
-- Run this against your validation DB. Adjust schema/user/host/port as necessary.

-- 1) portfolio_investor_details
INSERT INTO portfolio_investor_details (portfolio_id, name, phone_number, address) VALUES
('7839ee86-6527-43ed-94d6-53dadda2bd9a', 'Neha', 53456787, 'chennai'),
('7d08a041-6aed-421c-b895-1d94d4401b79', 'Bob', 5345678887, 'chennai'),
('7ecae4ee-a6df-4237-8e9a-5e4a5e417fa3', 'Kanishka', 554981126656, 'chennai'),
('8deeeb23-8862-4e29-8626-e538dddd868f', 'mm', 987667607103, 'England'),
('a1d62557-221d-4799-bfba-4d7215dcdac3', 'ram', 987667672103, 'England'),
('b23d70cf-6d7a-48b5-8e8c-4eda8f4d611d', 'Bob', 4345678887, 'chennai'),
('b94926b8-4919-4279-8470-1f3ec5b1b0fc', 'Tejo', 98981123456, 'chennai'),
('ca7377ab-2596-47a3-9c3d-4089206fa3de', 'Neharika', 554981123456, 'chennai'),
('d164c037-cf39-457e-99fb-9ec0b4cd47d8', 'Srikanth', 1234432111, 'Hyderabad'),
('d3a485c8-3e2f-4fa3-a823-254748942200', 'Suresh', 9898188556, 'chennai'),
('e9fc6225-6845-4c91-815f-ba3e9ea06e21', 'ram', 987667602103, 'England'),
('f6a225cc-c28a-4b6c-aecf-1fa00bc8255c', 'Guru', 989812234556, 'chennai');

-- Note: the entity mapping expects columns: portfolio_id (UUID), name, phone_number (bigint), address
-- If your DB has extra NOT NULL columns, adapt these INSERTs accordingly.

-- 2) pms_stocks
-- We'll set sector_name to 'TECH' for the sample symbols and populate timestamps
INSERT INTO pms_stocks (symbol, sector_name, created_at, updated_at) VALUES
('AAPL', 'TECH', now(), now()),
('MSFT', 'TECH', now(), now()),
('GOOGL', 'TECH', now(), now()),
('AMZN', 'TECH', now(), now()),
('META', 'TECH', now(), now()),
('NVDA', 'TECH', now(), now()),
('TSLA', 'AUTO', now(), now()),
('NFLX', 'ENTERTAINMENT', now(), now()),
('AMD', 'TECH', now(), now()),
('INTC', 'TECH', now(), now()),
('IBM', 'TECH', now(), now()),
('ORCL', 'TECH', now(), now()),
('BAC', 'FINANCE', now(), now()),
('JPM', 'FINANCE', now(), now()),
('WMT', 'RETAIL', now(), now());

-- End of seed script
