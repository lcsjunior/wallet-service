DELETE FROM wallet_transaction;
DELETE FROM idempotency_entry;
DELETE FROM wallet;
INSERT INTO wallet (id, user_id, balance, created_at, version)
VALUES ('35a907a7-9217-4e12-b1f2-5d80f579f9b0', '1a1b3c93-6d2f-4f7e-9a41-6c0f2a1d8e33', 100.00, CURRENT_TIMESTAMP, 0);
