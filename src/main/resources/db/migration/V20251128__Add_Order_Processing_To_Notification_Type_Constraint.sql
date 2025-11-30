-- Add ORDER_PROCESSING to notification_type CHECK constraint
-- ORDER_PROCESSING enum was added to NotificationTypeEnum but missing from database constraint

-- First, drop the existing constraint if it exists
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_notification_type_check;

-- Add the updated CHECK constraint with all enum values including ORDER_PROCESSING
ALTER TABLE notifications 
ADD CONSTRAINT notifications_notification_type_check 
CHECK (notification_type IN (
    'ORDER_CREATED',
    'ORDER_PROCESSING',
    'CONTRACT_READY',
    'CONTRACT_SIGNED',
    'PAYMENT_DEPOSIT_SUCCESS',
    'PAYMENT_FULL_SUCCESS',
    'DRIVER_ASSIGNED',
    'PICKING_UP_STARTED',
    'DELIVERY_STARTED',
    'DELIVERY_IN_PROGRESS',
    'DELIVERY_COMPLETED',
    'DELIVERY_FAILED',
    'ORDER_CANCELLED',
    'RETURN_STARTED',
    'RETURN_COMPLETED',
    'RETURN_PAYMENT_REQUIRED',
    'COMPENSATION_PROCESSED',
    'ISSUE_REPORTED',
    'ISSUE_IN_PROGRESS',
    'ISSUE_RESOLVED',
    'PACKAGE_DAMAGED',
    'ORDER_REJECTED_BY_RECEIVER',
    'REROUTE_REQUIRED',
    'SEAL_REPLACED',
    'SEAL_REPLACEMENT_COMPLETED',
    'SEAL_ASSIGNED',
    'PAYMENT_REMINDER',
    'PAYMENT_OVERDUE',
    'CONTRACT_SIGN_REMINDER',
    'CONTRACT_SIGN_OVERDUE',
    'STAFF_ORDER_CREATED',
    'STAFF_ORDER_PROCESSING',
    'STAFF_CONTRACT_SIGNED',
    'STAFF_DEPOSIT_RECEIVED',
    'STAFF_FULL_PAYMENT',
    'STAFF_RETURN_PAYMENT',
    'STAFF_ORDER_CANCELLED',
    'STAFF_PAYMENT_REMINDER',
    'PAYMENT_RECEIVED',
    'NEW_ORDER_ASSIGNED'
));

-- Comment for documentation
COMMENT ON CONSTRAINT notifications_notification_type_check ON notifications IS 'Ensures notification_type matches NotificationTypeEnum values including ORDER_PROCESSING';
