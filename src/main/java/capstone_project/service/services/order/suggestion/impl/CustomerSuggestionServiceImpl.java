package capstone_project.service.services.order.suggestion.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.response.order.ReceiverDetailResponse;
import capstone_project.dtos.response.order.ReceiverSuggestionResponse;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.service.mapper.order.ReceiverDetailMapper;
import capstone_project.service.mapper.order.ReceiverSuggestionMapper;
import capstone_project.service.services.order.suggestion.CustomerSuggestionService;
import capstone_project.utils.UserContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerSuggestionServiceImpl implements CustomerSuggestionService {

    private final OrderEntityService orderEntityService;
    private final UserContextUtils userContextUtils;
    private final ReceiverSuggestionMapper receiverSuggestionMapper;
    private final ReceiverDetailMapper receiverDetailMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ReceiverSuggestionResponse> getRecentReceivers(int limit) {
        log.info("Getting recent receivers with limit: {}", limit);
        UUID currentCustomerId = userContextUtils.getCurrentCustomerId();
        if (currentCustomerId == null) {
            log.error("Current customer ID is null");
            throw new BadRequestException(
                    ErrorEnum.USER_PERMISSION_DENIED.getMessage(),
                    ErrorEnum.USER_PERMISSION_DENIED.getErrorCode()
            );
        }

        // Get recent orders for the current customer
        List<OrderEntity> recentOrders = orderEntityService.findRecentOrdersByCustomerId(currentCustomerId, limit);
        log.info("Found {} recent orders for customer ID: {}", recentOrders.size(), currentCustomerId);

        // Map to receiver suggestions
        return recentOrders.stream()
                .filter(order -> order.getReceiverName() != null && order.getReceiverPhone() != null)
                .map(receiverSuggestionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiverDetailResponse getReceiverDetails(UUID orderId) {
        log.info("Getting receiver details for order ID: {}", orderId);
        // Find order by ID
        OrderEntity order = orderEntityService.findEntityById(orderId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + " Order with ID: " + orderId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Validate that the current customer is the sender of this order
        UUID currentCustomerId = userContextUtils.getCurrentCustomerId();
        if (currentCustomerId == null) {
            log.error("Current customer ID is null");
            throw new BadRequestException(
                    ErrorEnum.USER_PERMISSION_DENIED.getMessage(),
                    ErrorEnum.USER_PERMISSION_DENIED.getErrorCode()
            );
        }

        if (order.getSender() == null) {
            log.error("Order {} has no sender information", orderId);
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage() + " Sender information not found for order ID: " + orderId,
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        if (!currentCustomerId.equals(order.getSender().getId())) {
            log.error("Current customer ID {} does not match order sender ID {}",
                    currentCustomerId, order.getSender().getId());
            throw new BadRequestException(
                    ErrorEnum.USER_PERMISSION_DENIED.getMessage() + " You don't have permission to access this order",
                    ErrorEnum.USER_PERMISSION_DENIED.getErrorCode()
            );
        }

        log.info("Access authorized for customer ID: {} to order ID: {}", currentCustomerId, orderId);

        // Map to DTO
        return receiverDetailMapper.toDto(order);
    }
}
