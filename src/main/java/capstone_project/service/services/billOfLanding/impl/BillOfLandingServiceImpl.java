package capstone_project.service.services.billOfLanding.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.response.order.BillOfLandingResponse;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.order.CategoryEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.user.address.AddressEntity;
import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.service.mapper.order.OrderMapper;
import capstone_project.service.mapper.user.CustomerMapper;
import capstone_project.service.mapper.user.UserMapper;
import capstone_project.service.mapper.vehicle.VehicleAssignmentMapper;
import capstone_project.service.services.billOfLanding.BillOfLandingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillOfLandingServiceImpl implements BillOfLandingService {

    private final ContractEntityService contractEntityService;
//    private final VehicleEntityService vehicleEntityService;
//    private final VehicleAssignmentEntityService vehicleAssignmentEntityService;

    private final CustomerMapper customerMapper;
    private final OrderMapper orderMapper;
    private final VehicleAssignmentMapper vehicleAssignmentMapper;
    private final UserMapper userMapper;

    private final String BILL_OF_LANDING_PREFIX = "VN-TRUCKIE-";

    @Override
    public BillOfLandingResponse getBillOfLandingById(UUID contractId) {
        log.info("getBillOfLandingById");

        ContractEntity contractEntity = contractEntityService.findEntityById(contractId)
                .orElseThrow(() -> new NotFoundException(
                        "Contract not found with id: " + contractId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        OrderEntity orderEntity = contractEntity.getOrderEntity();
        UserEntity staff = contractEntity.getStaff();
        AddressEntity deliveryAddress = orderEntity.getDeliveryAddress();
        AddressEntity pickupAddress = orderEntity.getPickupAddress();
        CustomerEntity customer = orderEntity.getSender();
        CategoryEntity category = orderEntity.getCategory();

        List<OrderDetailEntity> orderDetails =
                Optional.ofNullable(orderEntity.getOrderDetailEntities())
                        .orElse(Collections.emptyList());

        List<VehicleAssignmentEntity> vehicleAssignments = orderEntity.getOrderDetailEntities().stream()
                .peek(orderDetail -> log.info("OrderDetail: {}", orderDetail))
                .map(OrderDetailEntity::getVehicleAssignmentEntity)
                .filter(Objects::nonNull)
                .toList();

        String datePart = new SimpleDateFormat("yyMMdd").format(new Date());
        String contractShort = contractId.toString().substring(0, 6).toUpperCase();
        String randomPart = RandomStringUtils.randomAlphanumeric(4).toUpperCase();

        String billOfLadingCode = BILL_OF_LANDING_PREFIX + datePart + "-" + contractShort + "-" + randomPart;

        return BillOfLandingResponse.builder()
                .id(UUID.randomUUID().toString())
                .code(billOfLadingCode)
                .staff(userMapper.mapUserResponse(staff))
                .customer(customerMapper.mapCustomerResponse(customer))
                .order(orderMapper.toGetOrderResponse(orderEntity))
                .vehicleAssignmentResponse(vehicleAssignments.stream()
                        .map(vehicleAssignmentMapper::toGetVehicleAssignmentForBillOfLandingResponse)
                        .toList())
                .createdAt(new Date().toString())
                .build();
    }
}
