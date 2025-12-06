package capstone_project.service.services.order.order;

import java.util.UUID;

/**
 * Context object for unified order cancellation operations
 * Encapsulates all information needed for order cancellation
 */
public class OrderCancellationContext {
    
    public enum CancellationType {
        CUSTOMER_CANCEL("Customer cancelled order"),
        STAFF_CANCEL("Staff cancelled order"),
        PAYMENT_TIMEOUT("Payment timeout - automatic cancellation"),
        CONTRACT_EXPIRY("Contract expired - automatic cancellation"),
        SYSTEM_CANCEL("System cancellation");
        
        private final String defaultReason;
        
        CancellationType(String defaultReason) {
            this.defaultReason = defaultReason;
        }
        
        public String getDefaultReason() {
            return defaultReason;
        }
    }
    
    private final CancellationType cancellationType;
    private final String customReason;
    private final UUID cancelledByUserId;
    private final boolean sendNotifications;
    private final boolean cleanupReservations;
    
    private OrderCancellationContext(Builder builder) {
        this.cancellationType = builder.cancellationType;
        this.customReason = builder.customReason;
        this.cancelledByUserId = builder.cancelledByUserId;
        this.sendNotifications = builder.sendNotifications;
        this.cleanupReservations = builder.cleanupReservations;
    }
    
    public CancellationType getCancellationType() {
        return cancellationType;
    }
    
    public String getCancellationReason() {
        return customReason != null ? customReason : cancellationType.getDefaultReason();
    }
    
    public UUID getCancelledByUserId() {
        return cancelledByUserId;
    }
    
    public boolean shouldSendNotifications() {
        return sendNotifications;
    }
    
    public boolean shouldCleanupReservations() {
        return cleanupReservations;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private CancellationType cancellationType;
        private String customReason;
        private UUID cancelledByUserId;
        private boolean sendNotifications = true;
        private boolean cleanupReservations = true;
        
        public Builder cancellationType(CancellationType cancellationType) {
            this.cancellationType = cancellationType;
            return this;
        }
        
        public Builder customReason(String customReason) {
            this.customReason = customReason;
            return this;
        }
        
        public Builder cancelledByUserId(UUID cancelledByUserId) {
            this.cancelledByUserId = cancelledByUserId;
            return this;
        }
        
        public Builder sendNotifications(boolean sendNotifications) {
            this.sendNotifications = sendNotifications;
            return this;
        }
        
        public Builder cleanupReservations(boolean cleanupReservations) {
            this.cleanupReservations = cleanupReservations;
            return this;
        }
        
        public OrderCancellationContext build() {
            if (cancellationType == null) {
                throw new IllegalArgumentException("Cancellation type is required");
            }
            return new OrderCancellationContext(this);
        }
    }
}
