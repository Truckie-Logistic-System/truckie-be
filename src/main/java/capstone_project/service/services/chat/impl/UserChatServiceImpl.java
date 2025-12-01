package capstone_project.service.services.chat.impl;

import capstone_project.common.enums.chat.*;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.chat.SendMessageRequest;
import capstone_project.dtos.response.chat.*;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.chat.ChatConversationEntity;
import capstone_project.entity.chat.ChatMessageEntity;
import capstone_project.entity.chat.ChatReadStatusEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.repository.repositories.auth.UserRepository;
import capstone_project.repository.repositories.chat.ChatConversationRepository;
import capstone_project.repository.repositories.chat.ChatMessageRepository;
import capstone_project.repository.repositories.chat.ChatReadStatusRepository;
import capstone_project.repository.repositories.order.order.OrderRepository;
import capstone_project.repository.repositories.vehicle.VehicleAssignmentRepository;
import capstone_project.repository.repositories.user.CustomerRepository;
import capstone_project.repository.repositories.user.DriverRepository;
import capstone_project.service.services.chat.UserChatService;
import capstone_project.service.services.cloudinary.CloudinaryService;
import capstone_project.service.rateLimit.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of user-to-user chat service
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserChatServiceImpl implements UserChatService {
    
    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatReadStatusRepository readStatusRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final DriverRepository driverRepository;
    private final OrderRepository orderRepository;
    private final VehicleAssignmentRepository vehicleAssignmentRepository;
    private final capstone_project.repository.repositories.order.order.OrderDetailRepository orderDetailRepository;
    private final capstone_project.repository.repositories.issue.IssueRepository issueRepository;
    private final capstone_project.repository.repositories.user.PenaltyHistoryRepository penaltyHistoryRepository;
    private final CloudinaryService cloudinaryService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RateLimitService rateLimitService;
    
    // ==================== Conversation Management ====================
    
    @Override
    public ChatConversationResponse createOrGetCustomerConversation(UUID customerId, UUID orderId) {
        log.info("Creating/getting customer conversation for customerId: {}, orderId: {}", customerId, orderId);
        
        CustomerEntity customer = customerRepository.findByUserId(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found", 15L));
        
        // Find existing active conversation
        Optional<ChatConversationEntity> existingConversation = conversationRepository
                .findByInitiatorIdAndStatusAndConversationType(
                        customerId,
                        ConversationStatusEnum.ACTIVE,
                        ConversationTypeEnum.CUSTOMER_SUPPORT
                );
        
        if (existingConversation.isPresent()) {
            ChatConversationEntity conv = existingConversation.get();
            // Update order context if provided
            if (orderId != null) {
                OrderEntity order = orderRepository.findById(orderId).orElse(null);
                if (order != null) {
                    conv.setCurrentOrder(order);
                    conv.setUpdatedAt(LocalDateTime.now());
                    conversationRepository.save(conv);
                }
            }
            return mapToConversationResponse(conv);
        }
        
        // Create new conversation
        ChatConversationEntity newConversation = ChatConversationEntity.builder()
                .conversationType(ConversationTypeEnum.CUSTOMER_SUPPORT)
                .initiator(customer.getUser())
                .initiatorType(ChatParticipantTypeEnum.CUSTOMER)
                .status(ConversationStatusEnum.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .unreadCount(0)
                .build();
        
        if (orderId != null) {
            OrderEntity order = orderRepository.findById(orderId).orElse(null);
            newConversation.setCurrentOrder(order);
        }
        
        ChatConversationEntity saved = conversationRepository.save(newConversation);
        log.info("Created new customer conversation with ID: {}", saved.getId());
        
        // Notify staff about new conversation
        broadcastNewConversation(saved);
        
        return mapToConversationResponse(saved);
    }
    
    @Override
    public ChatConversationResponse createOrGetDriverConversation(UUID driverId, UUID vehicleAssignmentId) {
        log.info("Creating/getting driver conversation for driverId: {}, vehicleAssignmentId: {}", driverId, vehicleAssignmentId);
        
        DriverEntity driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found", 15L));
        
        // Get user ID from driver entity - this is what's stored as initiator
        UUID userId = driver.getUser().getId();
        log.info("Looking for existing conversation with userId: {}", userId);
        
        // Find existing active conversation using userId (not driverId)
        Optional<ChatConversationEntity> existingConversation = conversationRepository
                .findByInitiatorIdAndStatusAndConversationType(
                        userId,
                        ConversationStatusEnum.ACTIVE,
                        ConversationTypeEnum.DRIVER_SUPPORT
                );
        
        if (existingConversation.isPresent()) {
            ChatConversationEntity conv = existingConversation.get();
            // Update vehicle assignment context if provided
            if (vehicleAssignmentId != null) {
                VehicleAssignmentEntity va = vehicleAssignmentRepository.findById(vehicleAssignmentId).orElse(null);
                if (va != null) {
                    conv.setCurrentVehicleAssignment(va);
                    conv.setUpdatedAt(LocalDateTime.now());
                    conversationRepository.save(conv);
                }
            }
            return mapToConversationResponse(conv);
        }
        
        // Create new conversation
        ChatConversationEntity newConversation = ChatConversationEntity.builder()
                .conversationType(ConversationTypeEnum.DRIVER_SUPPORT)
                .initiator(driver.getUser())
                .initiatorType(ChatParticipantTypeEnum.DRIVER)
                .status(ConversationStatusEnum.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .unreadCount(0)
                .build();
        
        if (vehicleAssignmentId != null) {
            VehicleAssignmentEntity va = vehicleAssignmentRepository.findById(vehicleAssignmentId).orElse(null);
            newConversation.setCurrentVehicleAssignment(va);
        }
        
        ChatConversationEntity saved = conversationRepository.save(newConversation);
        log.info("Created new driver conversation with ID: {}", saved.getId());
        
        // Notify staff about new conversation
        broadcastNewConversation(saved);
        
        return mapToConversationResponse(saved);
    }
    
    @Override
    public ChatConversationResponse createOrGetGuestConversation(String guestSessionId, String guestName) {
        log.info("Creating/getting guest conversation for sessionId: {}", guestSessionId);
        
        if (guestSessionId == null || guestSessionId.isBlank()) {
            guestSessionId = UUID.randomUUID().toString();
        }
        
        // Find existing active conversation
        Optional<ChatConversationEntity> existingConversation = conversationRepository
                .findByGuestSessionIdAndStatus(guestSessionId, ConversationStatusEnum.ACTIVE);
        
        if (existingConversation.isPresent()) {
            return mapToConversationResponse(existingConversation.get());
        }
        
        // Generate random guest ID (4 digits)
        String randomGuestId = String.format("%04d", new Random().nextInt(10000));
        String displayName = (guestName != null && !guestName.isBlank()) ? guestName : "Khách #" + randomGuestId;
        
        // Create new conversation
        ChatConversationEntity newConversation = ChatConversationEntity.builder()
                .conversationType(ConversationTypeEnum.GUEST_SUPPORT)
                .initiatorType(ChatParticipantTypeEnum.GUEST)
                .guestSessionId(guestSessionId)
                .guestName(displayName)
                .status(ConversationStatusEnum.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .unreadCount(0)
                .build();
        
        ChatConversationEntity saved = conversationRepository.save(newConversation);
        log.info("Created new guest conversation with ID: {} for guest: {}", saved.getId(), displayName);
        
        // Don't broadcast new conversation yet - only broadcast when first message is sent
        // broadcastNewConversation(saved);
        
        return mapToConversationResponse(saved);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ChatConversationResponse getConversation(UUID conversationId) {
        ChatConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found", 15L));
        return mapToConversationResponse(conversation);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ChatConversationResponse> getActiveConversations(String type, Pageable pageable) {
        Page<ChatConversationEntity> conversations;
        
        if (type != null && !type.isBlank()) {
            ConversationTypeEnum conversationType = ConversationTypeEnum.valueOf(type.toUpperCase());
            conversations = conversationRepository.findByConversationTypeAndStatusOrderByLastMessageAtDesc(
                    conversationType,
                    ConversationStatusEnum.ACTIVE,
                    pageable
            );
        } else {
            conversations = conversationRepository.findByStatusOrderByLastMessageAtDesc(
                    ConversationStatusEnum.ACTIVE,
                    pageable
            );
        }
        
        // Use staff-specific mapping to show correct unread count
        return conversations.map(this::mapToConversationResponseForStaff);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ChatConversationResponse> getUnreadConversations() {
        List<ChatConversationEntity> conversations = conversationRepository
                .findWithUnreadMessages(ConversationStatusEnum.ACTIVE);
        // Use staff-specific mapping for unread conversations list
        return conversations.stream()
                .map(this::mapToConversationResponseForStaff)
                .collect(Collectors.toList());
    }
    
    @Override
    public void closeConversation(UUID conversationId, UUID staffId) {
        log.info("Closing conversation: {} by staff: {}", conversationId, staffId);
        
        ChatConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found", 15L));
        
        UserEntity staff = userRepository.findById(staffId)
                .orElseThrow(() -> new NotFoundException("Staff not found", 15L));
        
        conversation.setStatus(ConversationStatusEnum.CLOSED);
        conversation.setClosedAt(LocalDateTime.now());
        conversation.setClosedBy(staff);
        conversation.setUpdatedAt(LocalDateTime.now());
        
        conversationRepository.save(conversation);
        
        // Send system message
        sendSystemMessage(conversationId, "Cuộc hội thoại đã được đóng bởi nhân viên hỗ trợ.");
        
        // Broadcast update
        messagingTemplate.convertAndSend("/topic/chat/conversation/" + conversationId + "/closed", true);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ChatConversationResponse> searchConversations(String keyword) {
        List<ChatConversationEntity> conversations = conversationRepository
                .searchByName(keyword, ConversationStatusEnum.ACTIVE);
        // Use staff-specific mapping for search results
        return conversations.stream()
                .map(this::mapToConversationResponseForStaff)
                .collect(Collectors.toList());
    }
    
    // ==================== Message Management ====================
    
    @Override
    public ChatUserMessageResponse sendMessage(SendMessageRequest request) {
        log.info("Sending message to conversation: {}", request.getConversationId());
        
        ChatConversationEntity conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new NotFoundException("Conversation not found", 15L));
        
        if (conversation.getStatus() != ConversationStatusEnum.ACTIVE) {
            throw new BadRequestException("Conversation is closed", 16L);
        }
        
        // Apply rate limiting based on conversation type
        String conversationIdStr = request.getConversationId().toString();
        if (conversation.getConversationType() == ConversationTypeEnum.GUEST_SUPPORT) {
            // Rate limit guest messages by conversation ID
            if (!rateLimitService.canSendGuestMessage(conversationIdStr, "websocket")) {
                throw new BadRequestException("Rate limit exceeded. Please wait before sending more messages.", 17L);
            }
        } else {
            // Rate limit authenticated user messages by conversation + user
            String userIdStr = request.getSenderId() != null ? request.getSenderId().toString() : "unknown";
            if (!rateLimitService.canSendAuthenticatedMessage(conversationIdStr, userIdStr)) {
                throw new BadRequestException("Rate limit exceeded. Please wait before sending more messages.", 18L);
            }
        }
        
        // Determine sender info
        UserEntity sender = null;
        ChatParticipantTypeEnum senderType;
        String senderName;
        
        if (request.getSenderId() != null) {
            sender = userRepository.findById(request.getSenderId()).orElse(null);
            if (sender != null) {
                String roleName = sender.getRole() != null ? sender.getRole().getRoleName() : "";
                senderType = determineSenderType(roleName);
                senderName = sender.getFullName();
            } else {
                senderType = ChatParticipantTypeEnum.GUEST;
                senderName = request.getSenderName() != null ? request.getSenderName() : "Khách";
            }
        } else {
            senderType = ChatParticipantTypeEnum.GUEST;
            senderName = request.getSenderName() != null ? request.getSenderName() : conversation.getGuestName();
        }
        
        // Create message
        ChatMessageEntity message = ChatMessageEntity.builder()
                .conversation(conversation)
                .sender(sender)
                .senderType(senderType)
                .senderName(senderName)
                .content(request.getContent())
                .messageType(request.getMessageType() != null ? 
                        ChatMessageTypeEnum.valueOf(request.getMessageType().toUpperCase()) : 
                        ChatMessageTypeEnum.TEXT)
                .imageUrl(request.getImageUrl())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        ChatMessageEntity savedMessage = messageRepository.save(message);
        
        // Update conversation
        String preview = request.getContent();
        if (preview.length() > 100) {
            preview = preview.substring(0, 100) + "...";
        }
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setLastMessagePreview(preview);
        
        // Increment unread count for staff when message is from customer/driver/guest
        // Note: unreadCount field is specifically for staff's unread messages
        // Customer/driver unread count is calculated dynamically from message.isRead
        if (senderType != ChatParticipantTypeEnum.STAFF) {
            conversation.setUnreadCount(conversation.getUnreadCount() + 1);
        }
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        
        ChatUserMessageResponse response = mapToMessageResponse(savedMessage);
        
        // Broadcast message to conversation subscribers
        messagingTemplate.convertAndSend("/topic/chat/conversation/" + conversation.getId(), response);
        
        // Notify all staff about new message
        if (senderType != ChatParticipantTypeEnum.STAFF) {
            broadcastNewMessageToStaff(conversation, response);
        }
        
        log.info("Message sent successfully with ID: {}", savedMessage.getId());
        return response;
    }
    
    @Override
    @Transactional(readOnly = true)
    public ChatMessagesPageResponse getMessages(UUID conversationId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatMessageEntity> messages = messageRepository
                .findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        
        List<ChatUserMessageResponse> messageResponses = messages.getContent().stream()
                .map(this::mapToMessageResponse)
                .collect(Collectors.toList());
        
        // Reverse to show oldest first
        Collections.reverse(messageResponses);
        
        UUID lastMessageId = messages.hasContent() ? 
                messages.getContent().get(messages.getContent().size() - 1).getId() : null;
        
        return ChatMessagesPageResponse.builder()
                .messages(messageResponses)
                .lastMessageId(lastMessageId)
                .hasMore(messages.hasNext())
                .totalCount((int) messages.getTotalElements())
                .build();
    }
    
    @Override
    public void markAsRead(UUID conversationId, UUID staffId) {
        log.info("Marking messages as read for conversation: {} by staff: {}", conversationId, staffId);
        
        int updated = messageRepository.markAsRead(conversationId, LocalDateTime.now(), staffId);
        
        // Reset unread count
        conversationRepository.resetUnreadCount(conversationId, LocalDateTime.now());
        
        log.info("Marked {} messages as read", updated);
        
        // Broadcast read status to the conversation via WebSocket for real-time UI update
        Map<String, Object> readStatus = new HashMap<>();
        readStatus.put("conversationId", conversationId);
        readStatus.put("readerId", staffId);
        readStatus.put("readerType", "STAFF"); // Add readerType so frontend knows who read
        readStatus.put("readAt", LocalDateTime.now().toString());
        readStatus.put("messagesRead", updated);
        
        messagingTemplate.convertAndSend("/topic/chat/conversation/" + conversationId + "/read", readStatus);
        log.info("Broadcasted read status to conversation: {}", conversationId);
    }
    
    @Override
    public void markAsReadForCustomer(UUID conversationId, UUID customerId) {
        log.info("Marking messages as read for conversation: {} by customer: {}", conversationId, customerId);
        
        // For customers, we mark staff messages as read
        int updated = messageRepository.markStaffMessagesAsReadForCustomer(conversationId, LocalDateTime.now());
        
        log.info("Marked {} staff messages as read for customer", updated);
        
        // Broadcast read status to the conversation via WebSocket for real-time UI update
        Map<String, Object> readStatus = new HashMap<>();
        readStatus.put("conversationId", conversationId);
        readStatus.put("readerId", customerId);
        readStatus.put("readerType", "CUSTOMER");
        readStatus.put("readAt", LocalDateTime.now().toString());
        readStatus.put("messagesRead", updated);
        
        messagingTemplate.convertAndSend("/topic/chat/conversation/" + conversationId + "/read", readStatus);
        log.info("Broadcasted customer read status to conversation: {}", conversationId);
    }
    
    @Override
    public void markAsReadForDriver(UUID conversationId, UUID driverId) {
        log.info("Marking messages as read for conversation: {} by driver: {}", conversationId, driverId);
        
        // For drivers, we mark staff messages as read
        int updated = messageRepository.markStaffMessagesAsReadForDriver(conversationId, LocalDateTime.now());
        
        log.info("Marked {} staff messages as read for driver", updated);
        
        // Broadcast read status to the conversation via WebSocket for real-time UI update
        Map<String, Object> readStatus = new HashMap<>();
        readStatus.put("conversationId", conversationId);
        readStatus.put("readerId", driverId);
        readStatus.put("readerType", "DRIVER");
        readStatus.put("readAt", LocalDateTime.now().toString());
        readStatus.put("messagesRead", updated);
        
        messagingTemplate.convertAndSend("/topic/chat/conversation/" + conversationId + "/read", readStatus);
        log.info("Broadcasted driver read status to conversation: {}", conversationId);
    }
    
    @Override
    public String uploadChatImage(MultipartFile file, UUID conversationId) throws IOException {
        log.info("Uploading chat image for conversation: {}", conversationId);
        
        byte[] fileBytes = file.getBytes();
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        
        Map<String, Object> uploadResult = cloudinaryService.uploadFile(fileBytes, fileName, "chat_images");
        String imageUrl = (String) uploadResult.get("secure_url");
        
        log.info("Image uploaded successfully: {}", imageUrl);
        return imageUrl;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ChatUserMessageResponse> searchMessages(UUID conversationId, String keyword) {
        List<ChatMessageEntity> messages = messageRepository.searchByContent(conversationId, keyword);
        return messages.stream()
                .map(this::mapToMessageResponse)
                .collect(Collectors.toList());
    }
    
    // ==================== Overview Data ====================
    
    @Override
    @Transactional(readOnly = true)
    public CustomerOverviewResponse getCustomerOverview(UUID customerId) {
        log.info("Getting customer overview for: {}", customerId);
        
        CustomerEntity customer = customerRepository.findByUserId(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found", 15L));
        
        UserEntity user = customer.getUser();
        
        // Get order statistics
        List<OrderEntity> orders = orderRepository.findBySenderIdOrderByCreatedAtDesc(customer.getId());
        int totalOrders = orders.size();
        long successfulOrders = orders.stream()
                .filter(o -> "SUCCESSFUL".equals(o.getStatus()) || "DELIVERED".equals(o.getStatus()))
                .count();
        long cancelledOrders = orders.stream()
                .filter(o -> "CANCELLED".equals(o.getStatus()))
                .count();
        
        double successRate = totalOrders > 0 ? (double) successfulOrders / totalOrders * 100 : 0;
        double cancelRate = totalOrders > 0 ? (double) cancelledOrders / totalOrders * 100 : 0;
        
        // Get latest orders regardless of status
        List<CustomerOverviewResponse.ActiveOrderInfo> activeOrders = orders.stream()
                .limit(3)
                .map(o -> CustomerOverviewResponse.ActiveOrderInfo.builder()
                        .orderId(o.getId())
                        .orderCode(o.getOrderCode())
                        .status(o.getStatus())
                        .receiverName(o.getReceiverName())
                        .driverName(getDriverNameForOrder(o))
                        .trackingCode(getTrackingCodeForOrder(o))
                        .createdAt(o.getCreatedAt())
                        .build())
                .toList();
        
        // Get recent orders
        List<CustomerOverviewResponse.RecentOrderInfo> recentOrders = orders.stream()
                .limit(10)
                .map(o -> CustomerOverviewResponse.RecentOrderInfo.builder()
                        .orderId(o.getId())
                        .orderCode(o.getOrderCode())
                        .status(o.getStatus())
                        .receiverName(o.getReceiverName())
                        .pickupAddress(formatAddress(o.getPickupAddress()))
                        .deliveryAddress(formatAddress(o.getDeliveryAddress()))
                        .totalQuantity(o.getTotalQuantity())
                        .createdAt(o.getCreatedAt())
                        .build())
                .toList();
        
        return CustomerOverviewResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .imageUrl(user.getImageUrl())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .memberSince(user.getCreatedAt())
                .customerId(customer.getId())
                .companyName(customer.getCompanyName())
                .representativeName(customer.getRepresentativeName())
                .representativePhone(customer.getRepresentativePhone())
                .businessLicenseNumber(customer.getBusinessLicenseNumber())
                .businessAddress(customer.getBusinessAddress())
                .customerStatus(customer.getStatus())
                .totalOrders(totalOrders)
                .successfulOrders((int) successfulOrders)
                .successRate(Math.round(successRate * 100.0) / 100.0)
                .cancelledOrders((int) cancelledOrders)
                .cancelRate(Math.round(cancelRate * 100.0) / 100.0)
                .issuesCount(0)
                .activeOrders(activeOrders)
                .recentOrders(recentOrders)
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public DriverOverviewResponse getDriverOverview(UUID driverId) {
        log.info("Getting driver overview for: {}", driverId);
        
        DriverEntity driver = driverRepository.findByUserId(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found", 15L));
        
        UserEntity user = driver.getUser();
        
        // Get vehicle assignments statistics
        List<VehicleAssignmentEntity> assignments = vehicleAssignmentRepository
                .findByPrimaryDriverIdOrSecondaryDriverIdOrderByCreatedAtDesc(driver.getId(), driver.getId());
        
        int totalTrips = assignments.size();
        long successfulTrips = assignments.stream()
                .filter(a -> "COMPLETED".equals(a.getStatus()))
                .count();
        long cancelledTrips = assignments.stream()
                .filter(a -> "CANCELLED".equals(a.getStatus()))
                .count();
        
        double successRate = totalTrips > 0 ? (double) successfulTrips / totalTrips * 100 : 0;
        double cancelRate = totalTrips > 0 ? (double) cancelledTrips / totalTrips * 100 : 0;
        
        // Get active trips
        List<String> activeStatuses = Arrays.asList("PICKING_UP", "ON_DELIVERED", "ONGOING_DELIVERED", "IN_TRANSIT");
        List<DriverOverviewResponse.ActiveTripInfo> activeTrips = assignments.stream()
                .filter(a -> activeStatuses.contains(a.getStatus()))
                .map(a -> DriverOverviewResponse.ActiveTripInfo.builder()
                        .vehicleAssignmentId(a.getId())
                        .trackingCode(a.getTrackingCode())
                        .status(a.getStatus())
                        .build())
                .toList();
        
        // Get recent trips
        List<DriverOverviewResponse.RecentTripInfo> recentTrips = assignments.stream()
                .limit(10)
                .map(a -> {
                    // Get orderCode from order details
                    String orderCode = null;
                    List<OrderDetailEntity> orderDetails = orderDetailRepository.findByVehicleAssignmentEntityId(a.getId());
                    if (orderDetails != null && !orderDetails.isEmpty()) {
                        OrderDetailEntity firstDetail = orderDetails.get(0);
                        if (firstDetail.getOrderEntity() != null) {
                            orderCode = firstDetail.getOrderEntity().getOrderCode();
                        }
                    }
                    
                    return DriverOverviewResponse.RecentTripInfo.builder()
                            .vehicleAssignmentId(a.getId())
                            .trackingCode(a.getTrackingCode())
                            .status(a.getStatus())
                            .orderCode(orderCode)
                            .vehicleType(a.getVehicleEntity() != null && a.getVehicleEntity().getVehicleTypeEntity() != null 
                                    ? a.getVehicleEntity().getVehicleTypeEntity().getDescription() : null)
                            .isActive("PICKING_UP".equals(a.getStatus()) || "ON_DELIVERED".equals(a.getStatus()) 
                                    || "ONGOING_DELIVERED".equals(a.getStatus()) || "IN_TRANSIT".equals(a.getStatus()))
                            .build();
                })
                .toList();
        
        return DriverOverviewResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .imageUrl(user.getImageUrl())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .memberSince(user.getCreatedAt())
                .driverId(driver.getId())
                .identityNumber(driver.getIdentityNumber())
                .driverLicenseNumber(driver.getDriverLicenseNumber())
                .licenseClass(driver.getLicenseClass())
                .dateOfExpiry(driver.getDateOfExpiry())
                .driverStatus(driver.getStatus())
                .totalOrdersReceived(totalTrips)
                .totalTripsCompleted((int) successfulTrips)
                .successfulDeliveries((int) successfulTrips)
                .successRate(Math.round(successRate * 100.0) / 100.0)
                .cancelledDeliveries((int) cancelledTrips)
                .cancelRate(Math.round(cancelRate * 100.0) / 100.0)
                .issuesCount((int) issueRepository.countByDriverId(driver.getId()))
                .penaltiesCount((int) penaltyHistoryRepository.countByDriverId(driver.getId()))
                .activeTrips(activeTrips)
                .recentTrips(recentTrips)
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public OrderQuickViewResponse getOrderQuickView(UUID orderId) {
        log.info("Getting order quick view for: {}", orderId);
        
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found", 15L));
        
        // Map order details
        List<OrderQuickViewResponse.OrderDetailInfo> orderDetails = order.getOrderDetailEntities().stream()
                .map(od -> OrderQuickViewResponse.OrderDetailInfo.builder()
                        .id(od.getId())
                        .trackingCode(od.getTrackingCode())
                        .status(od.getStatus())
                        .description(od.getDescription())
                        .weight(od.getWeightTons())
                        .weightBaseUnit(od.getWeightBaseUnit())
                        .unit(od.getUnit())
                        .declaredValue(od.getDeclaredValue())
                        .isFragile(false)
                        .build())
                .collect(java.util.stream.Collectors.toList());
        
        return OrderQuickViewResponse.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .notes(order.getNotes())
                .totalQuantity(order.getTotalQuantity())
                .createdAt(order.getCreatedAt())
                .customerName(order.getSender() != null && order.getSender().getUser() != null ? 
                        order.getSender().getUser().getFullName() : null)
                .customerPhone(order.getSender() != null && order.getSender().getUser() != null ? 
                        order.getSender().getUser().getPhoneNumber() : null)
                .companyName(order.getSender() != null ? order.getSender().getCompanyName() : null)
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .receiverIdentity(order.getReceiverIdentity())
                .pickupAddress(formatAddress(order.getPickupAddress()))
                .deliveryAddress(formatAddress(order.getDeliveryAddress()))
                .packageDescription(order.getPackageDescription())
                .categoryName(order.getCategory() != null ? order.getCategory().getCategoryName().name() : null)
                .categoryDescription(order.getCategory() != null ? order.getCategory().getDescription() : null)
                .hasInsurance(order.getHasInsurance())
                .totalDeclaredValue(order.getTotalDeclaredValue())
                .orderDetails(orderDetails)
                .build();
    }
    
    // ==================== Statistics ====================
    
    @Override
    @Transactional(readOnly = true)
    public ChatStatisticsResponse getChatStatistics() {
        long customerCount = conversationRepository.countByConversationTypeAndStatus(
                ConversationTypeEnum.CUSTOMER_SUPPORT, ConversationStatusEnum.ACTIVE);
        long driverCount = conversationRepository.countByConversationTypeAndStatus(
                ConversationTypeEnum.DRIVER_SUPPORT, ConversationStatusEnum.ACTIVE);
        long guestCount = conversationRepository.countByConversationTypeAndStatus(
                ConversationTypeEnum.GUEST_SUPPORT, ConversationStatusEnum.ACTIVE);
        
        Integer totalUnread = conversationRepository.countTotalUnreadMessages(ConversationStatusEnum.ACTIVE);
        
        return ChatStatisticsResponse.builder()
                .totalActiveConversations((int) (customerCount + driverCount + guestCount))
                .customerSupportCount((int) customerCount)
                .driverSupportCount((int) driverCount)
                .guestSupportCount((int) guestCount)
                .totalUnreadMessages(totalUnread != null ? totalUnread : 0)
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public int getUnreadCount() {
        Integer count = conversationRepository.countTotalUnreadMessages(ConversationStatusEnum.ACTIVE);
        return count != null ? count : 0;
    }
    
    // ==================== Private Helper Methods ====================
    
    /**
     * Map conversation for customer/driver/guest view
     * Shows unread count of STAFF messages
     */
    private ChatConversationResponse mapToConversationResponse(ChatConversationEntity entity) {
        // Calculate unread count dynamically for customer/driver/guest
        // Count unread messages from STAFF (senderType = STAFF and isRead = false)
        int customerUnreadCount = messageRepository.countUnreadStaffMessages(entity.getId());
        
        ChatConversationResponse.ChatConversationResponseBuilder builder = ChatConversationResponse.builder()
                .id(entity.getId())
                .conversationType(entity.getConversationType().name())
                .initiatorType(entity.getInitiatorType().name())
                .guestSessionId(entity.getGuestSessionId())
                .guestName(entity.getGuestName())
                .status(entity.getStatus().name())
                // Use customer unread count (staff messages not read by customer/driver)
                .unreadCount(customerUnreadCount)
                .lastMessageAt(entity.getLastMessageAt())
                .lastMessagePreview(entity.getLastMessagePreview())
                .createdAt(entity.getCreatedAt())
                .closedAt(entity.getClosedAt());
        
        return buildConversationResponse(builder, entity);
    }
    
    /**
     * Map conversation for staff view
     * Shows unread count of customer/driver/guest messages
     */
    private ChatConversationResponse mapToConversationResponseForStaff(ChatConversationEntity entity) {
        ChatConversationResponse.ChatConversationResponseBuilder builder = ChatConversationResponse.builder()
                .id(entity.getId())
                .conversationType(entity.getConversationType().name())
                .initiatorType(entity.getInitiatorType().name())
                .guestSessionId(entity.getGuestSessionId())
                .guestName(entity.getGuestName())
                .status(entity.getStatus().name())
                // Use entity unread count (customer/driver messages not read by staff)
                .unreadCount(entity.getUnreadCount())
                .lastMessageAt(entity.getLastMessageAt())
                .lastMessagePreview(entity.getLastMessagePreview())
                .createdAt(entity.getCreatedAt())
                .closedAt(entity.getClosedAt());
        
        return buildConversationResponse(builder, entity);
    }
    
    /**
     * Build common conversation response fields
     */
    private ChatConversationResponse buildConversationResponse(
            ChatConversationResponse.ChatConversationResponseBuilder builder,
            ChatConversationEntity entity) {
        
        if (entity.getInitiator() != null) {
            builder.initiatorId(entity.getInitiator().getId())
                    .initiatorName(entity.getInitiator().getFullName())
                    .initiatorImageUrl(entity.getInitiator().getImageUrl());
        }
        
        if (entity.getCurrentOrder() != null) {
            builder.currentOrderId(entity.getCurrentOrder().getId())
                    .currentOrderCode(entity.getCurrentOrder().getOrderCode());
        }
        
        if (entity.getCurrentVehicleAssignment() != null) {
            builder.currentVehicleAssignmentId(entity.getCurrentVehicleAssignment().getId())
                    .currentTrackingCode(entity.getCurrentVehicleAssignment().getTrackingCode());
        } else if (entity.getInitiatorType() == ChatParticipantTypeEnum.DRIVER && entity.getInitiator() != null) {
            // For driver conversations without vehicle assignment, try to find active assignment
            DriverEntity driver = driverRepository.findByUserId(entity.getInitiator().getId()).orElse(null);
            if (driver != null) {
                // Check both driver1 and driver2 assignments, get the most recent active one
                List<VehicleAssignmentEntity> allAssignments = vehicleAssignmentRepository
                        .findByPrimaryDriverIdOrSecondaryDriverIdOrderByCreatedAtDesc(driver.getId(), driver.getId());
                List<VehicleAssignmentEntity> activeAssignments = allAssignments.stream()
                        .filter(va -> "ACTIVE".equals(va.getStatus()))
                        .toList();
                
                if (!activeAssignments.isEmpty()) {
                    VehicleAssignmentEntity activeAssignment = activeAssignments.get(0);
                    builder.currentVehicleAssignmentId(activeAssignment.getId())
                            .currentTrackingCode(activeAssignment.getTrackingCode());
                }
            }
        }
        
        // Add active orders for customer
        if (entity.getInitiatorType() == ChatParticipantTypeEnum.CUSTOMER && entity.getInitiator() != null) {
            CustomerEntity customer = customerRepository.findByUserId(entity.getInitiator().getId()).orElse(null);
            if (customer != null) {
                List<OrderEntity> orders = orderRepository.findBySenderIdOrderByCreatedAtDesc(customer.getId());
                // Get all orders regardless of status, then limit to latest 3 for display
                List<ChatConversationResponse.ActiveOrderInfo> activeOrders = orders.stream()
                        .limit(3)
                        .map(o -> ChatConversationResponse.ActiveOrderInfo.builder()
                                .orderId(o.getId())
                                .orderCode(o.getOrderCode())
                                .status(o.getStatus())
                                .receiverName(o.getReceiverName())
                                .createdAt(o.getCreatedAt())
                                .modifiedAt(o.getModifiedAt())
                                .build())
                        .collect(Collectors.toList());
                builder.activeOrders(activeOrders);
            }
        }
        
        return builder.build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public VehicleAssignmentQuickViewResponse getVehicleAssignmentQuickView(UUID vehicleAssignmentId) {
        log.info("Getting vehicle assignment quick view for: {}", vehicleAssignmentId);
        
        VehicleAssignmentEntity va = vehicleAssignmentRepository.findById(vehicleAssignmentId)
                .orElseThrow(() -> new NotFoundException("Vehicle assignment not found", 15L));
        
        // Build response with tabs data
        VehicleAssignmentQuickViewResponse.VehicleAssignmentQuickViewResponseBuilder builder = 
                VehicleAssignmentQuickViewResponse.builder()
                        .vehicleAssignmentId(va.getId())
                        .trackingCode(va.getTrackingCode())
                        .status(va.getStatus())
                        .description(va.getDescription())
                        .createdAt(va.getCreatedAt());
        
        // Tab 1: Vehicle & Driver Info
        if (va.getVehicleEntity() != null) {
            var vehicle = va.getVehicleEntity();
            builder.vehicleInfo(VehicleAssignmentQuickViewResponse.VehicleInfo.builder()
                    .vehicleId(vehicle.getId())
                    .licensePlateNumber(vehicle.getLicensePlateNumber())
                    .model(vehicle.getModel())
                    .manufacturer(vehicle.getManufacturer())
                    .year(vehicle.getYear())
                    .capacity(vehicle.getCapacity())
                    .status(vehicle.getStatus())
                    .vehicleTypeName(vehicle.getVehicleTypeEntity() != null ? 
                            vehicle.getVehicleTypeEntity().getVehicleTypeName() : null)
                    .currentLatitude(vehicle.getCurrentLatitude())
                    .currentLongitude(vehicle.getCurrentLongitude())
                    .lastUpdated(vehicle.getLastUpdated())
                    .build());
        }
        
        if (va.getDriver1() != null) {
            builder.primaryDriver(mapToDriverInfo(va.getDriver1()));
        }
        if (va.getDriver2() != null) {
            builder.secondaryDriver(mapToDriverInfo(va.getDriver2()));
        }
        
        // Tab 2: Orders & Packages - Query order details linked to this vehicle assignment
        List<VehicleAssignmentQuickViewResponse.OrderInfo> ordersInfo = new ArrayList<>();
        List<capstone_project.entity.order.order.OrderDetailEntity> orderDetails = 
                orderDetailRepository.findActiveOrderDetailsByVehicleAssignmentId(
                        va.getId(), 
                        List.of("PENDING", "PICKING_UP", "IN_TRANSIT", "DELIVERED", "SUCCESSFUL"));
        
        if (orderDetails != null && !orderDetails.isEmpty()) {
            // Group by order
            Map<UUID, List<capstone_project.entity.order.order.OrderDetailEntity>> orderMap = 
                    orderDetails.stream()
                            .filter(od -> od.getOrderEntity() != null)
                            .collect(Collectors.groupingBy(od -> od.getOrderEntity().getId()));
            
            for (var entry : orderMap.entrySet()) {
                var firstOd = entry.getValue().get(0);
                var order = firstOd.getOrderEntity();
                
                List<VehicleAssignmentQuickViewResponse.PackageInfo> packages = entry.getValue().stream()
                        .map(od -> VehicleAssignmentQuickViewResponse.PackageInfo.builder()
                                .orderDetailId(od.getId())
                                .trackingCode(od.getTrackingCode())
                                .status(od.getStatus())
                                .description(od.getDescription())
                                .weightTons(od.getWeightTons())
                                .weightUnit("tấn")
                                .declaredValue(od.getDeclaredValue())
                                .sizeName(od.getOrderSizeEntity() != null ? 
                                        od.getOrderSizeEntity().getDescription() : null)
                                .build())
                        .toList();
                
                ordersInfo.add(VehicleAssignmentQuickViewResponse.OrderInfo.builder()
                        .orderId(order.getId())
                        .orderCode(order.getOrderCode())
                        .status(order.getStatus())
                        .categoryName(order.getCategory() != null ? 
                                order.getCategory().getCategoryName().name() : null)
                        .categoryDescription(order.getCategory() != null ? 
                                order.getCategory().getDescription() : null)
                        .senderName(order.getSender() != null && order.getSender().getUser() != null ? 
                                order.getSender().getUser().getFullName() : null)
                        .senderPhone(order.getSender() != null && order.getSender().getUser() != null ? 
                                order.getSender().getUser().getPhoneNumber() : null)
                        .pickupAddress(formatAddress(order.getPickupAddress()))
                        .pickupLatitude(order.getPickupAddress() != null ? 
                                order.getPickupAddress().getLatitude() : null)
                        .pickupLongitude(order.getPickupAddress() != null ? 
                                order.getPickupAddress().getLongitude() : null)
                        .receiverName(order.getReceiverName())
                        .receiverPhone(order.getReceiverPhone())
                        .deliveryAddress(formatAddress(order.getDeliveryAddress()))
                        .deliveryLatitude(order.getDeliveryAddress() != null ? 
                                order.getDeliveryAddress().getLatitude() : null)
                        .deliveryLongitude(order.getDeliveryAddress() != null ? 
                                order.getDeliveryAddress().getLongitude() : null)
                        .packages(packages)
                        .build());
            }
        }
        builder.orders(ordersInfo);
        
        // Tab 3: Issues
        List<capstone_project.entity.issue.IssueEntity> issues = 
                issueRepository.findAllByVehicleAssignmentEntity(va);
        if (issues != null && !issues.isEmpty()) {
            List<VehicleAssignmentQuickViewResponse.IssueInfo> issuesInfo = issues.stream()
                    .map(issue -> VehicleAssignmentQuickViewResponse.IssueInfo.builder()
                            .issueId(issue.getId())
                            .issueTypeName(issue.getIssueTypeEntity() != null ? 
                                    issue.getIssueTypeEntity().getIssueTypeName() : null)
                            .issueCategory(issue.getIssueTypeEntity() != null ? 
                                    issue.getIssueTypeEntity().getIssueCategory() : null)
                            .description(issue.getDescription())
                            .status(issue.getStatus())
                            .reportedAt(issue.getReportedAt())
                            .resolvedAt(issue.getResolvedAt())
                            .locationLatitude(issue.getLocationLatitude())
                            .locationLongitude(issue.getLocationLongitude())
                            .oldSealCode(issue.getOldSeal() != null ? 
                                    issue.getOldSeal().getSealCode() : null)
                            .newSealCode(issue.getNewSeal() != null ? 
                                    issue.getNewSeal().getSealCode() : null)
                            .sealRemovalImage(issue.getSealRemovalImage())
                            .build())
                    .toList();
            builder.issues(issuesInfo);
        }
        
        // Tab 4: Proofs & Seals - placeholder for now (would need additional repositories)
        builder.packingProofs(new ArrayList<>());
        builder.photoCompletions(new ArrayList<>());
        builder.seals(new ArrayList<>());
        
        // Tab 5: Journey - placeholder (would need JourneyHistoryRepository)
        builder.journeyHistory(new ArrayList<>());
        builder.journeySegments(new ArrayList<>());
        builder.fuelConsumption(null);
        
        return builder.build();
    }
    
    private VehicleAssignmentQuickViewResponse.DriverInfo mapToDriverInfo(
            capstone_project.entity.user.driver.DriverEntity driver) {
        if (driver == null) return null;
        var user = driver.getUser();
        return VehicleAssignmentQuickViewResponse.DriverInfo.builder()
                .driverId(driver.getId())
                .userId(user != null ? user.getId() : null)
                .fullName(user != null ? user.getFullName() : null)
                .phoneNumber(user != null ? user.getPhoneNumber() : null)
                .email(user != null ? user.getEmail() : null)
                .imageUrl(user != null ? user.getImageUrl() : null)
                .identityNumber(driver.getIdentityNumber())
                .driverLicenseNumber(driver.getDriverLicenseNumber())
                .licenseClass(driver.getLicenseClass())
                .status(driver.getStatus())
                .build();
    }
    
    private ChatUserMessageResponse mapToMessageResponse(ChatMessageEntity entity) {
        return ChatUserMessageResponse.builder()
                .id(entity.getId())
                .conversationId(entity.getConversation().getId())
                .senderId(entity.getSender() != null ? entity.getSender().getId() : null)
                .senderType(entity.getSenderType().name())
                .senderName(entity.getSenderName())
                .senderImageUrl(entity.getSender() != null ? entity.getSender().getImageUrl() : null)
                .content(entity.getContent())
                .messageType(entity.getMessageType().name())
                .imageUrl(entity.getImageUrl())
                .isRead(entity.getIsRead())
                .readAt(entity.getReadAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
    
    private ChatParticipantTypeEnum determineSenderType(String roleName) {
        if (roleName == null) return ChatParticipantTypeEnum.GUEST;
        return switch (roleName.toUpperCase()) {
            case "CUSTOMER" -> ChatParticipantTypeEnum.CUSTOMER;
            case "DRIVER" -> ChatParticipantTypeEnum.DRIVER;
            case "STAFF", "ADMIN" -> ChatParticipantTypeEnum.STAFF;
            default -> ChatParticipantTypeEnum.GUEST;
        };
    }
    
    private void broadcastNewConversation(ChatConversationEntity conversation) {
        // Use staff-specific mapping when broadcasting to staff
        ChatConversationResponse response = mapToConversationResponseForStaff(conversation);
        messagingTemplate.convertAndSend("/topic/chat/staff/new-conversation", response);
    }
    
    private void broadcastNewMessageToStaff(ChatConversationEntity conversation, ChatUserMessageResponse message) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("conversationId", conversation.getId());
        notification.put("conversationType", conversation.getConversationType().name());
        notification.put("initiatorName", conversation.getInitiator() != null ? 
                conversation.getInitiator().getFullName() : conversation.getGuestName());
        notification.put("message", message);
        
        messagingTemplate.convertAndSend("/topic/chat/staff/new-message", notification);
    }
    
    private void sendSystemMessage(UUID conversationId, String content) {
        ChatConversationEntity conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation == null) return;
        
        ChatMessageEntity message = ChatMessageEntity.builder()
                .conversation(conversation)
                .senderType(ChatParticipantTypeEnum.SYSTEM)
                .senderName("Hệ thống")
                .content(content)
                .messageType(ChatMessageTypeEnum.SYSTEM)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        messageRepository.save(message);
    }
    
    private String getDriverNameForOrder(OrderEntity order) {
        // Get first vehicle assignment's primary driver name
        if (order.getOrderDetailEntities() != null && !order.getOrderDetailEntities().isEmpty()) {
            return order.getOrderDetailEntities().stream()
                    .filter(od -> od.getVehicleAssignmentEntity() != null && 
                            od.getVehicleAssignmentEntity().getDriver1() != null)
                    .findFirst()
                    .map(od -> od.getVehicleAssignmentEntity().getDriver1().getUser().getFullName())
                    .orElse(null);
        }
        return null;
    }
    
    private String getTrackingCodeForOrder(OrderEntity order) {
        if (order.getOrderDetailEntities() != null && !order.getOrderDetailEntities().isEmpty()) {
            return order.getOrderDetailEntities().stream()
                    .filter(od -> od.getVehicleAssignmentEntity() != null)
                    .findFirst()
                    .map(od -> od.getVehicleAssignmentEntity().getTrackingCode())
                    .orElse(null);
        }
        return null;
    }
    
    private String formatAddress(capstone_project.entity.user.address.AddressEntity address) {
        if (address == null) return null;
        StringBuilder sb = new StringBuilder();
        if (address.getStreet() != null) sb.append(address.getStreet());
        if (address.getWard() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(address.getWard());
        }
        if (address.getProvince() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(address.getProvince());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}
