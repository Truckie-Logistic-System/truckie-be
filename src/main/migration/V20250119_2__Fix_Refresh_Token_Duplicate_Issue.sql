-- Migration to fix refresh token duplicate issues
-- This migration:
-- 1. Removes duplicate tokens keeping the most recent one
-- 2. Adds unique constraint on token column
-- 3. Adds index for performance

-- Step 1: Mark older duplicate tokens as revoked (keep the newest one per token)
UPDATE refresh_token rt1
SET revoked = true
WHERE EXISTS (
    SELECT 1
    FROM refresh_token rt2
    WHERE rt1.token = rt2.token
      AND rt1.created_at < rt2.created_at
);

-- Step 2: Add unique partial index (only for non-revoked tokens)
-- This allows duplicate token values only if they're revoked
CREATE UNIQUE INDEX idx_refresh_token_unique_active
    ON refresh_token(token)
    WHERE revoked = false;

-- Step 3: Add index for cleanup queries
CREATE INDEX idx_refresh_token_expired_at
    ON refresh_token(expired_at);

CREATE INDEX idx_refresh_token_revoked_created
    ON refresh_token(revoked, created_at);

-- Step 4: Add comment for future reference
COMMENT ON INDEX idx_refresh_token_unique_active IS 'Ensures unique active refresh tokens. Allows duplicates only for revoked tokens.';
