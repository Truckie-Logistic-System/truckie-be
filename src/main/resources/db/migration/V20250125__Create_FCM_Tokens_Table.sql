-- Create FCM tokens table for push notification management
CREATE TABLE IF NOT EXISTS fcm_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    device_type VARCHAR(20) NOT NULL CHECK (device_type IN ('ANDROID', 'IOS', 'WEB')),
    device_info VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP,
    expires_at TIMESTAMP,
    
    CONSTRAINT fcm_tokens_user_not_null CHECK (user_id IS NOT NULL)
);

-- Indexes for performance
CREATE INDEX idx_fcm_tokens_token ON fcm_tokens(token);
CREATE INDEX idx_fcm_tokens_user_id ON fcm_tokens(user_id);
CREATE INDEX idx_fcm_tokens_device_type ON fcm_tokens(device_type);
CREATE INDEX idx_fcm_tokens_is_active ON fcm_tokens(is_active);
CREATE INDEX idx_fcm_tokens_last_used_at ON fcm_tokens(last_used_at) WHERE last_used_at IS NOT NULL;
CREATE INDEX idx_fcm_tokens_expires_at ON fcm_tokens(expires_at) WHERE expires_at IS NOT NULL;

-- Unique constraint: One token per user device combination
CREATE UNIQUE INDEX idx_fcm_tokens_user_device_unique 
    ON fcm_tokens(user_id, device_type, token) 
    WHERE is_active = TRUE;

-- Comments for documentation
COMMENT ON TABLE fcm_tokens IS 'Stores FCM push notification tokens for users';
COMMENT ON COLUMN fcm_tokens.token IS 'FCM registration token from Firebase';
COMMENT ON COLUMN fcm_tokens.user_id IS 'Foreign key to users table (UUID type for customers/staff)';
COMMENT ON COLUMN fcm_tokens.device_type IS 'Device platform: ANDROID, IOS, or WEB';
COMMENT ON COLUMN fcm_tokens.device_info IS 'Optional device information (model, OS version, etc.)';
COMMENT ON COLUMN fcm_tokens.is_active IS 'Whether this token is currently active for push notifications';
COMMENT ON COLUMN fcm_tokens.last_used_at IS 'Timestamp when this token was last used to send notification';
COMMENT ON COLUMN fcm_tokens.expires_at IS 'Token expiration time (if applicable)';
