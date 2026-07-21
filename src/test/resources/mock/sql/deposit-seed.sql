DELETE FROM wallet_transaction;
DELETE FROM idempotency_entry;
DELETE FROM wallet;
INSERT INTO wallet (id, user_id, balance, created_at, version)
VALUES ('6163fb26-3a06-4080-a987-35c5e5a17297', '1a1b3c93-6d2f-4f7e-9a41-6c0f2a1d8e33', 0.00, CURRENT_TIMESTAMP, 0);
