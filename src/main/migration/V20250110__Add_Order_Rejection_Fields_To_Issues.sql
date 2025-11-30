-- Migration script for ORDER_REJECTION flow
-- Adds necessary fields to issues table for handling order rejection and return shipping

-- Add ORDER_REJECTION specific fields to issues table
ALTER TABLE issues
ADD COLUMN IF NOT EXISTS return_shipping_fee DECIMAL(19,2),
ADD COLUMN IF NOT EXISTS adjusted_return_fee DECIMAL(19,2),
ADD COLUMN IF NOT EXISTS return_transaction_id UUID,
ADD COLUMN IF NOT EXISTS return_journey_id UUID,
ADD COLUMN IF NOT EXISTS payment_deadline TIMESTAMP;

-- Add foreign key constraints
ALTER TABLE issues
ADD CONSTRAINT fk_issues_return_transaction
    FOREIGN KEY (return_transaction_id) 
    REFERENCES transaction(id) 
    ON DELETE SET NULL;

ALTER TABLE issues
ADD CONSTRAINT fk_issues_return_journey
    FOREIGN KEY (return_journey_id) 
    REFERENCES journey_history(id) 
    ON DELETE SET NULL;

-- Add comments for documentation
COMMENT ON COLUMN issues.return_shipping_fee IS 'Return shipping fee calculated by system';
COMMENT ON COLUMN issues.adjusted_return_fee IS 'Adjusted return fee by staff (overrides return_shipping_fee)';
COMMENT ON COLUMN issues.return_transaction_id IS 'Transaction for return shipping payment';
COMMENT ON COLUMN issues.return_journey_id IS 'New journey for return delivery';
COMMENT ON COLUMN issues.payment_deadline IS 'Payment deadline for return shipping fee';

-- Note: Order details affected by this issue are tracked via order_details.issue_id FK (existing relationship)
-- For ORDER_REJECTION: selected packages for return link to issue via issue_id
-- For DAMAGE: damaged packages link to issue via issue_id
-- Return delivery images are stored in issue_images table with description = 'RETURN_DELIVERY'
