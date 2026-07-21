DELETE FROM wallet_transaction;
DELETE FROM idempotency_entry;
DELETE FROM wallet;
INSERT INTO wallet (id, user_id, balance, created_at, version)
VALUES ('7bbda0fe-87ca-42a5-81df-2679d05f4b14', '1a1b3c93-6d2f-4f7e-9a41-6c0f2a1d8e33', 100.00, CURRENT_TIMESTAMP, 0);
INSERT INTO wallet (id, user_id, balance, created_at, version)
VALUES ('e79b9f63-59d1-4ede-a766-e6e68d53161d', '1a1b3c93-6d2f-4f7e-9a41-6c0f2a1d8e33', 0.00, CURRENT_TIMESTAMP, 0);
