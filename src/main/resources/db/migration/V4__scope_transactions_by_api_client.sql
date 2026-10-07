ALTER TABLE transactions ADD COLUMN IF NOT EXISTS client_id VARCHAR(100);
UPDATE transactions SET client_id = 'legacy' WHERE client_id IS NULL;
ALTER TABLE transactions ALTER COLUMN client_id SET NOT NULL;
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS uk_transactions_idempotency_key;
ALTER TABLE transactions ADD CONSTRAINT uk_transactions_client_idempotency_key UNIQUE (client_id, idempotency_key);
