-- CREATE EXTENSION IF NOT EXISTS postgres_fdw;

-- CREATE SERVER validation_server
--   FOREIGN DATA WRAPPER postgres_fdw
--   OPTIONS (host 'db-instance-pms.cvk4yqey0ex7.us-east-2.rds.amazonaws.com', dbname 'pms_validation_db', port '5432');

-- CREATE USER MAPPING FOR postgres
--   SERVER validation_server
--   OPTIONS (user 'postgres', password 'PMS.2025');

-- CREATE SCHEMA validation_fdw_schema;

-- IMPORT FOREIGN SCHEMA public
--   FROM SERVER validation_server
--   INTO validation_fdw_schema;

-- Dont follow below on
-- SELECT
--     'CREATE TABLE validation_' || table_name || ' AS SELECT * FROM validation_fdw_schema.' || table_name || ';'
-- FROM information_schema.tables
-- WHERE table_schema = 'validation_fdw_schema';

-- SELECT 
--     'CREATE TABLE validation_' || table_name || ' (LIKE validation_fdw_schema.' || table_name || ' INCLUDING ALL);' ||
--     'INSERT INTO validation_' || table_name || ' SELECT * FROM validation_fdw_schema.' || table_name || ';'
-- FROM information_schema.tables
-- WHERE table_schema = 'validation_fdw_schema';


-- DROP SCHEMA validation_fdw_schema CASCADE;
-- DROP SERVER validation_server CASCADE;
