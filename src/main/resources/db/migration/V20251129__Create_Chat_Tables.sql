-- =====================================================
-- Chat System Database Schema
-- Supports: Customer-Staff, Driver-Staff, Guest-Staff chat
-- =====================================================

-- Chat Conversations Table
CREATE TABLE chat_conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Conversation type: CUSTOMER_SUPPORT, DRIVER_SUPPORT, GUEST_SUPPORT
    conversation_type VARCHAR(30) NOT NULL,
    
    -- Initiator info (who started the conversation)
    initiator_id UUID NULL, -- NULL for guest
    initiator_type VARCHAR(20) NOT NULL, -- CUSTOMER, DRIVER, GUEST
    
    -- Guest-specific fields
    guest_session_id VARCHAR(100) NULL,
    guest_name VARCHAR(100) NULL,
    
    -- Context fields for better staff support
    current_order_id UUID NULL,
    current_vehicle_assignment_id UUID NULL,
    
    -- Status: ACTIVE, CLOSED, ARCHIVED
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    
    -- Last activity tracking
    last_message_at TIMESTAMP,
    last_message_preview VARCHAR(200),
    unread_count INTEGER DEFAULT 0,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    closed_at TIMESTAMP NULL,
    closed_by UUID NULL,
    
    -- Foreign keys
    CONSTRAINT fk_chat_conv_initiator FOREIGN KEY (initiator_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_chat_conv_order FOREIGN KEY (current_order_id) REFERENCES orders(id) ON DELETE SET NULL,
    CONSTRAINT fk_chat_conv_vehicle_assignment FOREIGN KEY (current_vehicle_assignment_id) REFERENCES vehicle_assignments(id) ON DELETE SET NULL,
    CONSTRAINT fk_chat_conv_closed_by FOREIGN KEY (closed_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Chat Messages Table
CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,
    
    -- Sender info
    sender_id UUID NULL, -- NULL for guest or system messages
    sender_type VARCHAR(20) NOT NULL, -- CUSTOMER, DRIVER, STAFF, GUEST, SYSTEM
    sender_name VARCHAR(100), -- Cached for quick display
    
    -- Message content
    content TEXT NOT NULL,
    message_type VARCHAR(20) DEFAULT 'TEXT' NOT NULL, -- TEXT, IMAGE, SYSTEM
    image_url VARCHAR(500) NULL,
    
    -- Read status
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    read_by_staff_id UUID NULL,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Foreign keys
    CONSTRAINT fk_chat_msg_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_msg_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_chat_msg_read_by FOREIGN KEY (read_by_staff_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Chat Read Status Table (Track which staff has read which messages)
CREATE TABLE chat_read_status (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL,
    staff_id UUID NOT NULL,
    read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    CONSTRAINT fk_chat_read_message FOREIGN KEY (message_id) REFERENCES chat_messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_read_staff FOREIGN KEY (staff_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_chat_read_status UNIQUE (message_id, staff_id)
);

-- Indexes for performance
CREATE INDEX idx_chat_conv_initiator ON chat_conversations(initiator_id);
CREATE INDEX idx_chat_conv_type ON chat_conversations(conversation_type);
CREATE INDEX idx_chat_conv_status ON chat_conversations(status);
CREATE INDEX idx_chat_conv_last_message ON chat_conversations(last_message_at DESC);
CREATE INDEX idx_chat_conv_guest_session ON chat_conversations(guest_session_id);

CREATE INDEX idx_chat_msg_conversation ON chat_messages(conversation_id);
CREATE INDEX idx_chat_msg_created ON chat_messages(created_at DESC);
CREATE INDEX idx_chat_msg_sender ON chat_messages(sender_id);
CREATE INDEX idx_chat_msg_unread ON chat_messages(conversation_id, is_read) WHERE is_read = FALSE;

CREATE INDEX idx_chat_read_message ON chat_read_status(message_id);
CREATE INDEX idx_chat_read_staff ON chat_read_status(staff_id);

-- Comments
COMMENT ON TABLE chat_conversations IS 'Stores chat conversations between users and staff';
COMMENT ON TABLE chat_messages IS 'Stores individual chat messages';
COMMENT ON TABLE chat_read_status IS 'Tracks which staff members have read which messages';
COMMENT ON COLUMN chat_conversations.conversation_type IS 'Type of conversation: CUSTOMER_SUPPORT, DRIVER_SUPPORT, GUEST_SUPPORT';
COMMENT ON COLUMN chat_conversations.initiator_type IS 'Who initiated: CUSTOMER, DRIVER, GUEST';
COMMENT ON COLUMN chat_messages.message_type IS 'Message type: TEXT, IMAGE, SYSTEM';
COMMENT ON COLUMN chat_messages.sender_type IS 'Who sent: CUSTOMER, DRIVER, STAFF, GUEST, SYSTEM';
