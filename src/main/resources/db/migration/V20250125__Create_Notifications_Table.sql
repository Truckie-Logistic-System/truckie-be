-- Create notifications table for persistent notification storage
-- Supports 3 roles: CUSTOMER, STAFF, DRIVER
-- Features: read tracking, email tracking, push notification tracking

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- User relationship
    user_id UUID NOT NULL,
    
    -- Role-based notification
    recipient_role VARCHAR(50) NOT NULL CHECK (recipient_role IN ('CUSTOMER', 'STAFF', 'DRIVER')),
    
    -- Content
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    
    -- Notification type
    notification_type VARCHAR(50) NOT NULL,
    
    -- Related entities (nullable)
    related_order_id UUID,
    related_order_detail_ids TEXT,  -- JSON array: ["uuid1", "uuid2"]
    related_issue_id UUID,
    related_vehicle_assignment_id UUID,
    related_contract_id UUID,
    
    -- Metadata (JSON for flexible additional data)
    metadata TEXT,
    
    -- Status
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    
    -- Email tracking (for customers)
    email_sent BOOLEAN NOT NULL DEFAULT FALSE,
    email_sent_at TIMESTAMP,
    
    -- Push notification tracking (for drivers)
    push_notification_sent BOOLEAN NOT NULL DEFAULT FALSE,
    push_notification_sent_at TIMESTAMP,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign keys
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_order FOREIGN KEY (related_order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_issue FOREIGN KEY (related_issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_vehicle_assignment FOREIGN KEY (related_vehicle_assignment_id) REFERENCES vehicle_assignments(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_contract FOREIGN KEY (related_contract_id) REFERENCES contracts(id) ON DELETE CASCADE
);

-- Indexes for efficient querying
CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_user_read_created ON notifications(user_id, is_read, created_at DESC);
CREATE INDEX idx_notifications_type_created ON notifications(notification_type, created_at DESC);
CREATE INDEX idx_notifications_order_id ON notifications(related_order_id);
CREATE INDEX idx_notifications_issue_id ON notifications(related_issue_id);
CREATE INDEX idx_notifications_recipient_role ON notifications(recipient_role);

-- Comment for documentation
COMMENT ON TABLE notifications IS 'Lưu trữ thông báo cho users (CUSTOMER, STAFF, DRIVER). Hỗ trợ email cho customer và push notification cho driver.';
COMMENT ON COLUMN notifications.recipient_role IS 'Role của người nhận: CUSTOMER, STAFF, hoặc DRIVER';
COMMENT ON COLUMN notifications.notification_type IS 'Loại thông báo theo NotificationTypeEnum';
COMMENT ON COLUMN notifications.related_order_detail_ids IS 'JSON array chứa danh sách OrderDetail UUIDs liên quan';
COMMENT ON COLUMN notifications.metadata IS 'JSON chứa thông tin bổ sung linh hoạt (orderCode, driverName, amounts, etc.)';
COMMENT ON COLUMN notifications.email_sent IS 'Tracking email đã gửi (dành cho CUSTOMER)';
COMMENT ON COLUMN notifications.push_notification_sent IS 'Tracking push notification đã gửi (dành cho DRIVER)';
