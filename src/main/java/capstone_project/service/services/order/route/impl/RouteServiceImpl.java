package capstone_project.service.services.order.route.impl;

import capstone_project.dtos.request.route.SuggestRouteRequest;
import capstone_project.dtos.response.route.RoutePointResponse;
import capstone_project.dtos.response.route.RoutePointsResponse;
import capstone_project.dtos.response.route.RouteSegmentResponse;
import capstone_project.dtos.response.route.SuggestRouteResponse;
import capstone_project.dtos.response.route.TollResponse;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.user.address.AddressEntity;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.setting.CarrierSettingEntityService;
import capstone_project.repository.entityServices.user.AddressEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.service.services.order.route.RouteService;
import capstone_project.service.services.thirdPartyServices.Vietmap.VietmapService;
import com.azure.core.implementation.jackson.ObjectMapperShim;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;

/**
 * Concrete RouteService implementation.
 * - Reads carrier coords from the single CareerSettings record.
 * - Resolves pickup/delivery coords using Address rows referenced by Order belonging to VehicleAssignment.
 * - All ids are UUID in DB and converted to string in responses.
 *
 * Replace mocked segment/toll logic with real routing/toll client calls.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private final CarrierSettingEntityService carrierSettingEntityService;
    private final VehicleAssignmentEntityService vehicleAssignmentEntityService;
    private final OrderEntityService orderEntityService;
    private final AddressEntityService addressEntityService;
    private final VietmapService vietmapService;

    @Override
    @Transactional(readOnly = true)
    public RoutePointsResponse getRoutePoints(UUID assignmentId) {
        if (assignmentId == null) {
            throw new IllegalArgumentException("assignmentId must not be null");
        }

        var careerOpt = carrierSettingEntityService.findAll().stream().findFirst();
        var career = careerOpt.orElseThrow(() -> new IllegalStateException("Carrier settings not found"));

        var va = vehicleAssignmentEntityService.findById(assignmentId)
                .orElseThrow(() -> new NoSuchElementException("VehicleAssignment not found: " + assignmentId));

        // Load Order from vehicle assignment
        var orderOpt = orderEntityService.findVehicleAssignmentOrder(assignmentId);
        OrderEntity order = orderOpt.orElseThrow(() -> new NoSuchElementException("Order not found for VehicleAssignment: " + assignmentId));

        // Prefer relationship getters on Order: getPickupAddress() / getDeliveryAddress()
        var pickupRef = order.getPickupAddress();
        var deliveryRef = order.getDeliveryAddress();
        if (pickupRef == null || deliveryRef == null) {
            throw new IllegalStateException("Order must have pickup and delivery addresses: " + order.getId());
        }

        AddressEntity pickupAddr = addressEntityService.findById(pickupRef.getId())
                .orElseThrow(() -> new NoSuchElementException("Pickup address not found: " + pickupRef.getId()));
        AddressEntity deliveryAddr = addressEntityService.findById(deliveryRef.getId())
                .orElseThrow(() -> new NoSuchElementException("Delivery address not found: " + deliveryRef.getId()));

        List<RoutePointResponse> points = new ArrayList<>();
        points.add(new RoutePointResponse(
                "Carrier",
                "carrier",
                career.getCarrierLatitude(),    // or career.getLat()
                career.getCarrierLongitude(),   // or career.getLng()
                career.getCarrierAddressLine() == null ? "" : career.getCarrierAddressLine(),
                career.getId() == null ? null : career.getId()
        ));
        points.add(new RoutePointResponse(
                "Pickup",
                "pickup",
                pickupAddr.getLatitude(),    // or getLat()
                pickupAddr.getLongitude(),   // or getLng()
                resolveFullAddress(pickupAddr), // or getAddressLine()
                pickupAddr.getId()
        ));
        points.add(new RoutePointResponse(
                "Delivery",
                "delivery",
                deliveryAddr.getLatitude(),
                deliveryAddr.getLongitude(),
                resolveFullAddress(deliveryAddr),
                deliveryAddr.getId()
        ));

        String tracking = va.getTrackingCode() == null ? "" : va.getTrackingCode();
        return new RoutePointsResponse(assignmentId, tracking, points);
    }

    @Override
    @Transactional(readOnly = true)
    public RoutePointsResponse getRoutePointsByOrder(UUID orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }

        // Get carrier settings
        var careerOpt = carrierSettingEntityService.findAll().stream().findFirst();
        var career = careerOpt.orElseThrow(() -> new IllegalStateException("Carrier settings not found"));

        // Load the order directly - using findEntityById instead of findById
        OrderEntity order = orderEntityService.findEntityById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found with ID: " + orderId));

        // Get pickup and delivery addresses
        var pickupRef = order.getPickupAddress();
        var deliveryRef = order.getDeliveryAddress();
        if (pickupRef == null || deliveryRef == null) {
            throw new IllegalStateException("Order must have pickup and delivery addresses: " + order.getId());
        }

        AddressEntity pickupAddr = addressEntityService.findById(pickupRef.getId())
                .orElseThrow(() -> new NoSuchElementException("Pickup address not found: " + pickupRef.getId()));
        AddressEntity deliveryAddr = addressEntityService.findById(deliveryRef.getId())
                .orElseThrow(() -> new NoSuchElementException("Delivery address not found: " + deliveryRef.getId()));

        // Create the route points (carrier, pickup, delivery)
        List<RoutePointResponse> points = new ArrayList<>();
        points.add(new RoutePointResponse(
                "Carrier",
                "carrier",
                career.getCarrierLatitude(),
                career.getCarrierLongitude(),
                career.getCarrierAddressLine() == null ? "" : career.getCarrierAddressLine(),
                career.getId() == null ? null : career.getId()
        ));
        points.add(new RoutePointResponse(
                "Pickup",
                "pickup",
                pickupAddr.getLatitude(),
                pickupAddr.getLongitude(),
                resolveFullAddress(pickupAddr),
                pickupAddr.getId()
        ));
        points.add(new RoutePointResponse(
                "Delivery",
                "delivery",
                deliveryAddr.getLatitude(),
                deliveryAddr.getLongitude(),
                resolveFullAddress(deliveryAddr),
                deliveryAddr.getId()
        ));

        // Use order code or ID as reference
        String tracking = order.getOrderCode();
        if (tracking == null || tracking.isEmpty()) {
            tracking = order.getId().toString();
        }

        return new RoutePointsResponse(orderId, tracking, points);
    }

    @Override
    public SuggestRouteResponse suggestRoute(SuggestRouteRequest request) {
        if (request == null || request.points() == null) {
            throw new IllegalArgumentException("request.points must not be null");
        }

        List<List<BigDecimal>> rawPoints = request.points();
        if (rawPoints.size() < 2) {
            throw new IllegalArgumentException("At least two points required");
        }

        // convert BigDecimal points to List<List<Double>> for Vietmap request and validate ranges
        List<List<Double>> pathForRequest = new ArrayList<>();
        for (int i = 0; i < rawPoints.size(); i++) {
            List<BigDecimal> p = rawPoints.get(i);
            if (p == null || p.size() < 2) {
                throw new IllegalArgumentException("Each point must be a \\[lng, lat\\] pair. invalid index: " + i);
            }
            BigDecimal lngBd = p.get(0);
            BigDecimal latBd = p.get(1);
            if (lngBd == null || latBd == null) {
                throw new IllegalArgumentException("Longitude and latitude must not be null at index " + i);
            }
            double lng = lngBd.doubleValue();
            double lat = latBd.doubleValue();
            if (lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) {
                throw new IllegalArgumentException("Invalid coordinate range at index " + i);
            }
            pathForRequest.add(Arrays.asList(lng, lat)); // Vietmap expects numeric (double) values
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String bodyJson = objectMapper.writeValueAsString(Map.of("path", pathForRequest));

            Integer vietVehicle = null;
            // resolve vietVehicle if needed using request.vehicleTypeId() and mapVehicleTypeNameToVietmap(...)

            String vietResp = vietmapService.routeTolls(bodyJson, vietVehicle);
            JsonNode root = objectMapper.readTree(vietResp);

            // parse path into BigDecimal coordinates
            List<List<BigDecimal>> path = new ArrayList<>();
            JsonNode pathNode = root.path("path");
            if (pathNode.isArray()) {
                for (JsonNode coord : pathNode) {
                    if (coord.isArray() && coord.size() >= 2) {
                        BigDecimal lng = coord.get(0).decimalValue();
                        BigDecimal lat = coord.get(1).decimalValue();
                        path.add(Arrays.asList(lng, lat));
                    }
                }
            }

            // extract tolls and compute total
            List<TollResponse> tolls = new ArrayList<>();
            long totalToll = 0L;
            JsonNode tollsNode = root.path("tolls");
            if (tollsNode.isArray()) {
                for (JsonNode t : tollsNode) {
                    String name = t.path("name").asText(null);
                    String address = t.path("address").asText(null);
                    String type = t.path("type").asText(null);
                    long amount = t.path("amount").asLong(0L);
                    tolls.add(new TollResponse(name, address, type, amount));
                    totalToll += amount;
                }
            }

            RouteSegmentResponse seg = new RouteSegmentResponse(
                    1,
                    "Start",
                    "End",
                    path,
                    tolls,
                    Map.of("source", "vietmap")
            );

            return new SuggestRouteResponse(List.of(seg), totalToll);
        } catch (Exception ex) {
            // fallback: build mocked segments using original BigDecimal points (rawPoints)
            List<RouteSegmentResponse> segments = new ArrayList<>();
            long totalToll = 0L;
            for (int i = 0; i < rawPoints.size() - 1; i++) {
                List<BigDecimal> start = rawPoints.get(i);
                List<BigDecimal> end = rawPoints.get(i + 1);
                List<List<BigDecimal>> path = Arrays.asList(
                        Arrays.asList(start.get(0), start.get(1)),
                        Arrays.asList(end.get(0), end.get(1))
                );
                List<TollResponse> emptyTolls = Collections.emptyList();
                Map<String, Object> raw = Map.of("mock", true, "segmentIndex", i, "vehicleTypeId", request.vehicleTypeId());
                RouteSegmentResponse seg = new RouteSegmentResponse(
                        i + 1,
                        "Point " + i,
                        "Point " + (i + 1),
                        path,
                        emptyTolls,
                        raw
                );
                segments.add(seg);
            }
            return new SuggestRouteResponse(segments, totalToll);
        }
    }

    /**
     * Map local vehicle type name/code to Vietmap vehicle parameter.
     * Example mapping based on gross tonnage rules:
     * 1 -> trucks < 2 tons
     * 2 -> trucks < 4 tons
     * 3 -> trucks < 10 tons
     * 4 -> trucks < 18 tons
     * 5 -> trucks >= 18 tons
     */
    private Integer mapVehicleTypeNameToVietmap(String vehicleTypeName) {
        if (vehicleTypeName == null) return null;
        return switch (vehicleTypeName) {
            case "TRUCK_0_5_TON", "TRUCK_1_25_TON", "TRUCK_1_9_TON" -> 1;
            case "TRUCK_2_4_TONN" -> 2;
            case "TRUCK_3_5_TON", "TRUCK_5_TON", "TRUCK_7_TON" -> 3;
            case "TRUCK_10_TON" -> 4;
            // extend mapping for heavier trucks if present:
            // case "TRUCK_20_TON": return 5;
            default -> null;
        };
    }

    private String resolveFullAddress(Object addr) {
        if (addr == null) return "";
        try {
            // prefer full-address getters
            String[] fullGetters = {"getFullAddress", "getAddressLine", "getAddress", "getFormattedAddress"};
            for (String g : fullGetters) {
                try {
                    Method m = addr.getClass().getMethod(g);
                    Object v = m.invoke(addr);
                    if (v != null) {
                        String s = v.toString().trim();
                        if (!s.isEmpty()) return s;
                    }
                } catch (NoSuchMethodException ignored) {
                }
            }

            // otherwise build from street + ward + province (specific requirement)
            String[] partGetters = {"getStreet", "getWard", "getProvince"};
            List<String> parts = new ArrayList<>();
            for (String p : partGetters) {
                try {
                    Method m = addr.getClass().getMethod(p);
                    Object v = m.invoke(addr);
                    if (v != null) {
                        String s = v.toString().trim();
                        if (!s.isEmpty()) parts.add(s);
                    }
                } catch (NoSuchMethodException ignored) {
                }
            }
            return String.join(", ", parts);
        } catch (Exception ex) {
            try {
                String s = addr.toString();
                return s == null ? "" : s;
            } catch (Exception e) {
                return "";
            }
        }
    }
}
