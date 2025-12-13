package capstone_project.service.services.order.order.impl;

import capstone_project.dtos.response.order.contract.PriceCalculationResponse;
import capstone_project.entity.category.CategoryEntity;
import capstone_project.entity.category.CategoryPricingDetailEntity;
import capstone_project.entity.order.OrderDetailEntity;
import capstone_project.entity.order.OrderEntity;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.contract.ContractRuleEntity;
import capstone_project.entity.user.address.AddressEntity;
import capstone_project.entity.vehicle.SizeRuleEntity;
import capstone_project.entity.vehicle.VehicleTypeEntity;
import capstone_project.enums.CategoryName;
import capstone_project.service.services.category.CategoryPricingDetailEntityService;
import capstone_project.service.services.insurance.InsuranceCalculationService;
import capstone_project.service.services.map.VietMapDistanceService;
import capstone_project.service.services.map.impl.VietMapDistanceServiceImpl;
import capstone_project.service.services.order.detail.OrderDetailEntityService;
import capstone_project.service.services.pricing.UnifiedPricingService;
import capstone_project.service.services.vehicle.SizeRuleEntityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class ContractServiceImplTest {

    @Mock
    private VietMapDistanceService vietMapDistanceService;

    @Mock
    private SizeRuleEntityService sizeRuleEntityService;

    @Mock
    private UnifiedPricingService unifiedPricingService;

    @Mock
    private CategoryPricingDetailEntityService categoryPricingDetailEntityService;

    @Mock
    private InsuranceCalculationService insuranceCalculationService;

    @Mock
    private OrderDetailEntityService orderDetailEntityService;

    @InjectMocks
    private ContractServiceImpl contractService;

    private ContractEntity contract;
    private OrderEntity order;
    private CategoryEntity category;
    private AddressEntity pickupAddress;
    private AddressEntity deliveryAddress;
    private SizeRuleEntity sizeRule;
    private VehicleTypeEntity vehicleType;
    private Map<UUID, Integer> vehicleCountMap;
    private UUID sizeRuleId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create test data
        sizeRuleId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        // Create addresses
        pickupAddress = new AddressEntity();
        pickupAddress.setLatitude(new BigDecimal("10.762622"));
        pickupAddress.setLongitude(new BigDecimal("106.660172"));

        deliveryAddress = new AddressEntity();
        deliveryAddress.setLatitude(new BigDecimal("10.798088"));
        deliveryAddress.setLongitude(new BigDecimal("106.668617"));

        // Create category
        category = new CategoryEntity();
        category.setId(categoryId);
        category.setCategoryName(CategoryName.NORMAL);

        // Create order
        order = new OrderEntity();
        order.setId(orderId);
        order.setCategory(category);
        order.setPickupAddress(pickupAddress);
        order.setDeliveryAddress(deliveryAddress);
        order.setHasInsurance(false);

        // Create contract
        contract = new ContractEntity();
        contract.setId(contractId);
        contract.setOrderEntity(order);

        // Create vehicle type
        vehicleType = new VehicleTypeEntity();
        vehicleType.setVehicleTypeName("TRUCK_5_TON");

        // Create size rule
        sizeRule = new SizeRuleEntity();
        sizeRule.setId(sizeRuleId);
        sizeRule.setSizeRuleName("5 Ton Truck");
        sizeRule.setVehicleTypeEntity(vehicleType);

        // Create vehicle count map
        vehicleCountMap = new HashMap<>();
        vehicleCountMap.put(sizeRuleId, 1);

        // Mock service responses
        BigDecimal distanceKm = new BigDecimal("10.5");
        when(vietMapDistanceService.calculateDistance(any(AddressEntity.class), any(AddressEntity.class), anyString()))
                .thenReturn(distanceKm);

        // Mock distance and toll result
        VietMapDistanceServiceImpl.DistanceResult distanceResult = new VietMapDistanceServiceImpl.DistanceResult(
                distanceKm,
                new BigDecimal("40000.00"),
                2
        );
        when(vietMapDistanceService.calculateDistanceAndTolls(any(AddressEntity.class), any(AddressEntity.class), anyString()))
                .thenReturn(distanceResult);

        // Mock size rule service
        when(sizeRuleEntityService.findEntityById(sizeRuleId)).thenReturn(java.util.Optional.of(sizeRule));

        // Mock unified pricing service
        UnifiedPricingService.TierCalculationResult tierResult = new UnifiedPricingService.TierCalculationResult(
                "0-10km", new BigDecimal("50000"), new BigDecimal("10"), new BigDecimal("500000")
        );
        UnifiedPricingService.UnifiedPriceResult priceResult = new UnifiedPricingService.UnifiedPriceResult(
                true, new BigDecimal("500000"), new BigDecimal("500000"), new BigDecimal("500000"),
                Collections.singletonList(tierResult), null
        );
        when(unifiedPricingService.calculatePrice(eq(sizeRuleId), any(BigDecimal.class), eq(1), eq(categoryId)))
                .thenReturn(priceResult);

        // Mock category pricing detail
        CategoryPricingDetailEntity pricingDetail = new CategoryPricingDetailEntity();
        pricingDetail.setPriceMultiplier(BigDecimal.ONE);
        pricingDetail.setExtraFee(BigDecimal.ZERO);
        when(categoryPricingDetailEntityService.findByCategoryId(categoryId)).thenReturn(pricingDetail);

        // Mock insurance calculation
        when(insuranceCalculationService.isFragileCategory(any(CategoryName.class))).thenReturn(false);
        when(insuranceCalculationService.getInsuranceRateForDisplay(false)).thenReturn(new BigDecimal("0.08"));
        when(insuranceCalculationService.getVatRate()).thenReturn(new BigDecimal("10.00"));
        when(orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(orderId)).thenReturn(Collections.emptyList());
    }

    @Test
    void testCalculateTotalPriceWithTollFees() {
        // Call the method under test
        PriceCalculationResponse response = contractService.calculateTotalPrice(contract, new BigDecimal("10.5"), vehicleCountMap);

        // Verify the response
        assertNotNull(response);
        assertEquals(new BigDecimal("500000"), response.getTotalPrice());
        assertEquals(new BigDecimal("500000"), response.getFinalTotal());
        
        // Verify toll information is included
        assertEquals(new BigDecimal("40000.00"), response.getTotalTollFee());
        assertEquals(Integer.valueOf(2), response.getTotalTollCount());
        assertEquals("TRUCK_5_TON", response.getVehicleType());
        
        // Verify steps
        assertNotNull(response.getSteps());
        assertEquals(1, response.getSteps().size());
        assertEquals("5 Ton Truck", response.getSteps().get(0).getSizeRuleName());
    }
}
