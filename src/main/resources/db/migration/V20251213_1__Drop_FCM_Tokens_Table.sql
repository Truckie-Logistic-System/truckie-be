-- Drop FCM tokens table as it's no longer needed
DROP TABLE IF EXISTS fcm_tokens;

-- Drop related indexes if they exist
DROP INDEX IF EXISTS idx_fcm_tokens_token;
DROP INDEX IF EXISTS idx_fcm_tokens_user_id;
DROP INDEX IF EXISTS idx_fcm_tokens_device_type;
DROP INDEX IF EXISTS idx_fcm_tokens_is_active;
DROP INDEX IF EXISTS idx_fcm_tokens_last_used_at;
DROP INDEX IF EXISTS idx_fcm_tokens_expires_at;
DROP INDEX IF EXISTS idx_fcm_tokens_user_device_unique;
