package capstone_project.service.services.map;

import capstone_project.entity.user.address.AddressEntity;
import capstone_project.service.services.map.impl.VietMapDistanceServiceImpl;
import capstone_project.service.services.thirdPartyServices.Vietmap.VietmapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class VietMapDistanceServiceTest {

    @Mock
    private VietmapService vietmapService;

    @InjectMocks
    private VietMapDistanceServiceImpl vietMapDistanceService;

    private AddressEntity fromAddress;
    private AddressEntity toAddress;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create test addresses
        fromAddress = new AddressEntity();
        fromAddress.setLatitude(new BigDecimal("10.762622"));  // Ho Chi Minh City
        fromAddress.setLongitude(new BigDecimal("106.660172"));
        fromAddress.setStreet("Nguyen Hue");
        fromAddress.setWard("Ben Nghe");
        fromAddress.setProvince("Ho Chi Minh City");

        toAddress = new AddressEntity();
        toAddress.setLatitude(new BigDecimal("10.798088"));  // Tan Son Nhat Airport
        toAddress.setLongitude(new BigDecimal("106.668617"));
        toAddress.setStreet("Truong Son");
        toAddress.setWard("Ward 2");
        toAddress.setProvince("Ho Chi Minh City");

        // Mock VietMap API response for route-tolls
        String mockResponse = "{\n" +
                "  \"path\": [\n" +
                "    [106.660172, 10.762622],\n" +
                "    [106.661234, 10.765432],\n" +
                "    [106.664567, 10.776543],\n" +
                "    [106.668617, 10.798088]\n" +
                "  ],\n" +
                "  \"tolls\": [\n" +
                "    {\n" +
                "      \"name\": \"Toll Station 1\",\n" +
                "      \"address\": \"Highway 1\",\n" +
                "      \"type\": \"ETC\",\n" +
                "      \"amount\": 15000\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"Toll Station 2\",\n" +
                "      \"address\": \"Highway 2\",\n" +
                "      \"type\": \"BOT\",\n" +
                "      \"amount\": 25000\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        when(vietmapService.routeTolls(any(), any())).thenReturn(mockResponse);
    }

    @Test
    void testCalculateDistance() {
        // Test with default vehicle type
        BigDecimal distance = vietMapDistanceService.calculateDistance(fromAddress, toAddress);
        assertNotNull(distance);
        assertTrue(distance.compareTo(BigDecimal.ZERO) > 0);
        
        // Test with specific vehicle type
        distance = vietMapDistanceService.calculateDistance(fromAddress, toAddress, "truck");
        assertNotNull(distance);
        assertTrue(distance.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testCalculateDistanceAndTolls() {
        // Test with truck vehicle type
        VietMapDistanceServiceImpl.DistanceResult result = 
            vietMapDistanceService.calculateDistanceAndTolls(fromAddress, toAddress, "truck");
        
        assertNotNull(result);
        assertNotNull(result.getDistanceKm());
        assertTrue(result.getDistanceKm().compareTo(BigDecimal.ZERO) > 0);
        
        // Verify toll information
        assertEquals(2, result.getTotalTollCount());
        assertEquals(new BigDecimal("40000.00"), result.getTotalTollFee());
    }
}
