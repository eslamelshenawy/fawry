-- =====================================================================
-- Sample gateways and billers from the task brief.
-- Default users are seeded by DataInitializer on first startup
-- (password hashes are computed with the configured BCrypt encoder).
-- =====================================================================

INSERT INTO gateway (code, name, fixed_fee, percentage_fee, daily_limit,
                     min_transaction, max_transaction, processing_time_minutes,
                     available_24x7, available_days, available_from_hour, available_to_hour)
VALUES
    ('GW1', 'Gateway 1', 2.00, 0.0150,  50000.00, 10.00,  5000.00,    0,
     TRUE,  NULL, NULL, NULL),
    ('GW2', 'Gateway 2', 5.00, 0.0080, 200000.00, 100.00, NULL,    1440,
     FALSE, 'SUN,MON,TUE,WED,THU', 9, 17),
    ('GW3', 'Gateway 3', 0.00, 0.0250, 100000.00, 50.00,  10000.00, 120,
     TRUE,  NULL, NULL, NULL);

INSERT INTO biller (code, name) VALUES
    ('BILL_12345', 'Demo Biller One'),
    ('BILL_67890', 'Demo Biller Two');
