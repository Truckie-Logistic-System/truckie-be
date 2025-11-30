package capstone_project.service.services.ai;

import capstone_project.entity.user.address.AddressEntity;
import capstone_project.entity.issue.IssueEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.auth.UserEntity;
import capstone_project.repository.repositories.auth.UserRepository;
import capstone_project.repository.repositories.user.CustomerRepository;
import capstone_project.repository.repositories.order.order.OrderRepository;
import capstone_project.repository.repositories.user.AddressRepository;
import capstone_project.repository.repositories.issue.IssueRepository;
import capstone_project.entity.user.customer.CustomerEntity;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service to fetch customer personal data from database for AI chatbot
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerDataService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final IssueRepository issueRepository;

    private static final NumberFormat VND_FORMAT = NumberFormat.getInstance(new Locale("vi", "VN"));

    /**
     * Generate personalized customer information for AI
     */
    public String generateCustomerInfo(String userId) {
        log.info("🔍 DEBUG: generateCustomerInfo called with userId: {}", userId);
        
        if (userId == null || userId.isEmpty()) {
            return "⚠️ **Khách vãng lai (chưa đăng nhập)**: Đăng nhập để xem thông tin cá nhân, lịch sử đơn hàng, và địa chỉ đã lưu.\n\n";
        }

        try {
            UUID userUUID = UUID.fromString(userId);
            log.info("🔍 DEBUG: Parsed UUID: {}", userUUID);
            
            // First check if user exists
            UserEntity user = userRepository.findById(userUUID).orElse(null);
            log.info("🔍 DEBUG: User found in database: {}", user != null);
            
            if (user == null) {
                log.warn("⚠️ User not found in database for UUID: {}", userUUID);
                return "⚠️ Không tìm thấy thông tin người dùng.\n\n";
            }

            StringBuilder info = new StringBuilder();
            info.append("# 👤 THÔNG TIN KHÁCH HÀNG\n\n");

            // 1. Basic Info
            info.append("## Thông Tin Cơ Bản\n");
            info.append(String.format("- **Họ tên**: %s\n", user.getFullName() != null ? user.getFullName() : "Chưa cập nhật"));
            info.append(String.format("- **Email**: %s\n", user.getEmail()));
            info.append(String.format("- **Số điện thoại**: %s\n", user.getPhoneNumber() != null ? user.getPhoneNumber() : "Chưa cập nhật"));
            info.append("\n");

            // 2. Addresses - First find customer by user ID, then get addresses
            Optional<CustomerEntity> customerOpt = customerRepository.findByUserId(userUUID);
            if (customerOpt.isEmpty()) {
                log.warn("⚠️ No customer found for user_id: {}", userUUID);
                info.append("## 📍 Địa Chỉ Đã Lưu (0 địa chỉ)\n");
                info.append("- Không tìm thấy thông tin khách hàng.\n\n");
            } else {
                CustomerEntity customer = customerOpt.get();
                UUID customerId = customer.getId();
                log.info("📍 DEBUG: Found customer_id: {} for user_id: {}", customerId, userUUID);
                
                log.info("📍 DEBUG: Querying addresses with customer_Id: {}", customerId);
                List<AddressEntity> addresses = addressRepository.findByCustomer_Id(customerId);
                log.info("📍 DEBUG: Found {} addresses for customer {}", addresses.size(), customerId);
            
            // Additional debug: Check if ANY addresses exist in database
                List<AddressEntity> allAddresses = addressRepository.findAll();
                log.info("📍 DEBUG: Total addresses in database: {}", allAddresses.size());
                if (!allAddresses.isEmpty()) {
                    allAddresses.forEach(addr -> 
                        log.info("📍 DEBUG: Address exists - customer_id: {}, address: {}", 
                            addr.getCustomer() != null ? addr.getCustomer().getId() : "null", 
                            addr.getStreet())
                    );
                }
                // Group addresses by type
                List<AddressEntity> pickupAddresses = addresses.stream()
                        .filter(addr -> addr.getAddressType() != null && addr.getAddressType())
                        .collect(Collectors.toList());
                
                List<AddressEntity> deliveryAddresses = addresses.stream()
                        .filter(addr -> addr.getAddressType() != null && !addr.getAddressType())
                        .collect(Collectors.toList());
                
                List<AddressEntity> unclassifiedAddresses = addresses.stream()
                        .filter(addr -> addr.getAddressType() == null)
                        .collect(Collectors.toList());
                
                // Display pickup addresses
                info.append(String.format("## 🏭 Địa Chỉ Lấy Hàng (%d địa chỉ)\n", pickupAddresses.size()));
                if (pickupAddresses.isEmpty()) {
                    info.append("- Chưa có địa chỉ lấy hàng nào được lưu\n");
                } else {
                    for (AddressEntity addr : pickupAddresses) {
                        String fullAddress = buildFullAddress(addr);
                        log.info("📍 DEBUG: Pickup address: {}", fullAddress);
                        info.append(String.format("- %s\n", fullAddress));
                    }
                }
                info.append("\n");
                
                // Display delivery addresses
                info.append(String.format("## 🏠 Địa Chỉ Nhận Hàng (%d địa chỉ)\n", deliveryAddresses.size()));
                if (deliveryAddresses.isEmpty()) {
                    info.append("- Chưa có địa chỉ nhận hàng nào được lưu\n");
                } else {
                    for (AddressEntity addr : deliveryAddresses) {
                        String fullAddress = buildFullAddress(addr);
                        log.info("📍 DEBUG: Delivery address: {}", fullAddress);
                        info.append(String.format("- %s\n", fullAddress));
                    }
                }
                info.append("\n");
                
                // Display unclassified addresses
                if (!unclassifiedAddresses.isEmpty()) {
                    info.append(String.format("## 📍 Địa Chỉ Chưa Phân Loại (%d địa chỉ)\n", unclassifiedAddresses.size()));
                    for (AddressEntity addr : unclassifiedAddresses) {
                        String fullAddress = buildFullAddress(addr);
                        log.info("📍 DEBUG: Unclassified address: {}", fullAddress);
                        info.append(String.format("- %s\n", fullAddress));
                    }
                    info.append("\n");
                }
            }

            log.info("✅ Generated customer info for user: {}", userId);
            return info.toString();

        } catch (Exception e) {
            log.error("❌ Error generating customer info", e);
            return "⚠️ Không thể tải thông tin khách hàng. Vui lòng thử lại sau.\n\n";
        }
    }

    /**
     * Translate order status to Vietnamese
     */
    private String translateOrderStatus(String status) {
        return switch (status) {
            case "PENDING_QUOTE" -> "Chờ báo giá";
            case "PENDING_SIGNATURE" -> "Chờ ký hợp đồng";
            case "PENDING_DEPOSIT" -> "Chờ đặt cọc";
            case "PENDING_ASSIGNMENT" -> "Chờ phân công";
            case "PENDING_PAYMENT" -> "Chờ thanh toán";
            case "READY_FOR_PICKUP" -> "Sẵn sàng lấy hàng";
            case "IN_TRANSIT" -> "Đang vận chuyển";
            case "DELIVERED" -> "Đã giao hàng";
            case "COMPLETED" -> "Hoàn thành";
            case "CANCELLED" -> "Đã hủy";
            default -> status;
        };
    }

    /**
     * Translate issue category to Vietnamese
     */
    private String translateIssueCategory(String category) {
        return switch (category) {
            case "DAMAGE" -> "Hư hỏng hàng hóa";
            case "PENALTY" -> "Vi phạm";
            case "SEAL_REPLACEMENT" -> "Thay thế seal";
            case "ORDER_REJECTION" -> "Từ chối nhận hàng";
            case "REROUTE" -> "Tái định tuyến";
            default -> category;
        };
    }

    /**
     * Translate issue status to Vietnamese
     */
    private String translateIssueStatus(String status) {
        return switch (status) {
            case "OPEN" -> "Đang mở";
            case "IN_PROGRESS" -> "Đang xử lý";
            case "RESOLVED" -> "Đã giải quyết";
            case "CLOSED" -> "Đã đóng";
            default -> status;
        };
    }

    /**
     * Build full address from AddressEntity
     */
    private String buildFullAddress(AddressEntity addr) {
        StringBuilder address = new StringBuilder();
        if (addr.getStreet() != null && !addr.getStreet().isEmpty()) {
            address.append(addr.getStreet());
        }
        if (addr.getWard() != null && !addr.getWard().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(addr.getWard());
        }
        if (addr.getProvince() != null && !addr.getProvince().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(addr.getProvince());
        }
        return address.length() > 0 ? address.toString() : "Địa chỉ chưa đầy đủ";
    }
}
