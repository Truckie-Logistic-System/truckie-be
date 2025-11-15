package capstone_project.service.services.route.impl;

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
import capstone_project.service.services.route.RouteService;
import capstone_project.service.services.map.VietMapDistanceService;
import capstone_project.service.services.thirdPartyServices.Vietmap.VietmapService;
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
    private final VietMapDistanceService vietMapDistanceService;
    private final capstone_project.repository.entityServices.issue.IssueEntityService issueEntityService;

    @Override
    @Transactional(readOnly = true)
    public RoutePointsResponse getRoutePointsByIssue(UUID issueId) {
        if (issueId == null) {
            throw new IllegalArgumentException("issueId must not be null");
        }

        // Load issue to get order information
        var issue = issueEntityService.findEntityById(issueId)
                .orElseThrow(() -> new NoSuchElementException("Issue not found: " + issueId));

        // Get order from vehicle assignment
        var vehicleAssignment = issue.getVehicleAssignmentEntity();
        if (vehicleAssignment == null) {
            throw new IllegalStateException("Issue must have an associated vehicle assignment: " + issueId);
        }

        var orderOpt = orderEntityService.findVehicleAssignmentOrder(vehicleAssignment.getId());
        var order = orderOpt.orElseThrow(() -> new NoSuchElementException("Order not found for vehicle assignment: " + vehicleAssignment.getId()));

        // Get carrier settings
        var careerOpt = carrierSettingEntityService.findAll().stream().findFirst();
        var career = careerOpt.orElseThrow(() -> new IllegalStateException("Carrier settings not found"));

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

        // Build return route points: Carrier → Pickup → Delivery → Pickup (Return) → Carrier (Return)
        List<RoutePointResponse> points = new ArrayList<>();
        
        // Point 1: Carrier (start)
        points.add(new RoutePointResponse(
                "Carrier",
                "carrier",
                career.getCarrierLatitude(),
                career.getCarrierLongitude(),
                career.getCarrierAddressLine() == null ? "" : career.getCarrierAddressLine(),
                career.getId() == null ? null : career.getId()
        ));
        
        // Point 2: Pickup (đi lấy hàng)
        points.add(new RoutePointResponse(
                "Pickup",
                "pickup",
                pickupAddr.getLatitude(),
                pickupAddr.getLongitude(),
                resolveFullAddress(pickupAddr),
                pickupAddr.getId()
        ));
        
        // Point 3: Delivery (đi giao hàng bị từ chối)
        points.add(new RoutePointResponse(
                "Delivery",
                "delivery",
                deliveryAddr.getLatitude(),
                deliveryAddr.getLongitude(),
                resolveFullAddress(deliveryAddr),
                deliveryAddr.getId()
        ));
        
        // Point 4: Pickup Return (trả hàng về người gửi)
        points.add(new RoutePointResponse(
                "Pickup (Return)",
                "pickup",
                pickupAddr.getLatitude(),
                pickupAddr.getLongitude(),
                resolveFullAddress(pickupAddr),
                pickupAddr.getId()
        ));
        
        // Point 5: Carrier Return (quay về kho)
        points.add(new RoutePointResponse(
                "Carrier (Return)",
                "carrier",
                career.getCarrierLatitude(),
                career.getCarrierLongitude(),
                career.getCarrierAddressLine() == null ? "" : career.getCarrierAddressLine(),
                career.getId() == null ? null : career.getId()
        ));

        log.info("Generated return route points for issue {}: {} points", issueId, points.size());
        return new RoutePointsResponse(points);
    }

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

        // Return only the points list
        return new RoutePointsResponse(points);
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

        // Load the order directly - using findById instead of findEntityById
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

        // Return only the points list
        return new RoutePointsResponse(points);
    }

    @Override
    public SuggestRouteResponse suggestRoute(SuggestRouteRequest request) {
        if (request == null || request.points() == null) {
            throw new IllegalArgumentException("request.points must not be null");
        }

        List<List<BigDecimal>> pointsToUse = request.points();
        if (pointsToUse.size() < 2) {
            throw new IllegalArgumentException("At least two points required");
        }

        // Get original point types if provided
        List<String> typesToUse = request.pointTypes();

        // Handle case when pointTypes is null or has different size than points
        if (typesToUse == null || typesToUse.size() != pointsToUse.size()) {
            typesToUse = generateDefaultPointTypes(pointsToUse.size());
        }

        // convert BigDecimal points to List<List<Double>> for Vietmap request and validate ranges
        List<List<Double>> pathForRequest = new ArrayList<>();
        for (int i = 0; i < pointsToUse.size(); i++) {
            List<BigDecimal> p = pointsToUse.get(i);
            if (p == null || p.size() < 2) {
                throw new IllegalArgumentException("Each point must be a [lng, lat] pair. invalid index: " + i);
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
            // Send the path as a direct array of coordinates, not wrapped in a "path" object
            String bodyJson = objectMapper.writeValueAsString(pathForRequest);

            log.info("Sending Vietmap request body: {}", bodyJson);

            Integer vietVehicle = null;
            // resolve vietVehicle if needed using request.vehicleTypeId() and mapVehicleTypeIdToVietmap(...)
            if (request.vehicleTypeId() != null) {
                // Here you would map vehicle type ID to the appropriate Vietmap vehicle type
                // vietVehicle = mapVehicleTypeIdToVietmap(request.vehicleTypeId());
            }

            String vietResp = vietmapService.routeTolls(bodyJson, vietVehicle);
            log.info("Received Vietmap response: {}", vietResp);

            JsonNode root = objectMapper.readTree(vietResp);

            // parse path into BigDecimal coordinates
            List<List<BigDecimal>> path = new ArrayList<>();
            JsonNode pathNode = root.path("path");
            log.info("Path node exists: {}, isArray: {}, size: {}",
                    !pathNode.isMissingNode(),
                    pathNode.isArray(),
                    pathNode.isArray() ? pathNode.size() : 0);

            if (pathNode.isArray()) {
                for (JsonNode coord : pathNode) {
                    if (coord.isArray() && coord.size() >= 2) {
                        BigDecimal lng = coord.get(0).decimalValue();
                        BigDecimal lat = coord.get(1).decimalValue();
                        path.add(Arrays.asList(lng, lat));
                    }
                }
                log.info("Extracted path size: {}", path.size());
            } else {
                log.warn("Path node is not an array or is missing in the response");
            }

            // extract tolls and compute total
            List<TollResponse> tolls = new ArrayList<>();
            long totalTollAmount = 0L;
            int totalTollCount = 0;
            JsonNode tollsNode = root.path("tolls");
            log.info("Tolls node exists: {}, isArray: {}, size: {}",
                    !tollsNode.isMissingNode(),
                    tollsNode.isArray(),
                    tollsNode.isArray() ? tollsNode.size() : 0);

            if (tollsNode.isArray()) {
                for (JsonNode t : tollsNode) {
                    String name = t.path("name").asText(null);
                    String address = t.path("address").asText(null);
                    String type = t.path("type").asText(null);
                    long amount = t.path("amount").asLong(0L);

                    log.info("Toll extracted - Name: {}, Address: {}, Type: {}, Amount: {}",
                             name, address, type, amount);

                    // Create and add the toll response object
                    TollResponse toll = new TollResponse(name, address, type, amount);
                    tolls.add(toll);
                    totalTollAmount += amount;
                    totalTollCount++;
                }
                log.info("Extracted {} tolls with total amount: {}", totalTollCount, totalTollAmount);
            } else {
                log.warn("Tolls node is not an array or is missing in the response");
            }

            // Create separate segments for each point-to-point part of the route
            List<RouteSegmentResponse> segments = new ArrayList<>();

            // Create the raw data for response
            Map<String, Object> rawData = new HashMap<>();
            rawData.put("source", "vietmap");
            if (typesToUse != null && !typesToUse.isEmpty()) {
                rawData.put("pointTypes", typesToUse);
            }

            // If we have enough points, create multiple segments
            if (pointsToUse.size() > 2) {
                // Create a segment for each adjacent pair of points
                double totalDistance = 0;
                for (int i = 0; i < pointsToUse.size() - 1; i++) {
                    String fromType = typesToUse.get(i);
                    String toType = typesToUse.get(i + 1);

                    Map<String, Object> segmentRawData = new HashMap<>(rawData);
                    segmentRawData.put("fromType", fromType);
                    segmentRawData.put("toType", toType);
                    segmentRawData.put("segmentIndex", i);

                    List<List<BigDecimal>> segmentPath = extractPathSegment(path, pointsToUse.get(i), pointsToUse.get(i + 1));
                    // Calculate distance between waypoints using VietMap API (not the entire detailed path)
                    double segmentDistance = calculateDistanceVietMap(pointsToUse.get(i), pointsToUse.get(i + 1));
                    totalDistance += segmentDistance;

                    // Extract start and end coordinates
                    List<BigDecimal> startPoint = pointsToUse.get(i);
                    List<BigDecimal> endPoint = pointsToUse.get(i + 1);

                    RouteSegmentResponse segment = new RouteSegmentResponse(
                        i + 1,
                        formatPointLabel(fromType, i),
                        formatPointLabel(toType, i + 1),
                        startPoint.get(1),  // startLat
                        startPoint.get(0),  // startLng
                        endPoint.get(1),    // endLat
                        endPoint.get(0),    // endLng
                        segmentPath,
                        new ArrayList<>(), // Tolls will be distributed
                        segmentDistance,   // Add the segment distance
                        segmentRawData
                    );

                    segments.add(segment);
                }

                // Distribute toll costs across segments
                distributeTollsToSegments(segments, tolls, totalTollAmount);

                log.info("Creating SuggestRouteResponse with segments: {}, totalTollAmount: {}, totalTollCount: {}, totalDistance: {} km",
                    segments.size(), totalTollAmount, totalTollCount, totalDistance);
                return new SuggestRouteResponse(segments, totalTollAmount, totalTollCount, totalDistance);
            } else {
                // Just create a single segment for the whole route
                // Calculate distance between start and end waypoints
                double totalDistance = calculateDistanceVietMap(pointsToUse.get(0), pointsToUse.get(pointsToUse.size() - 1));

                // Extract start and end coordinates
                List<BigDecimal> startPoint = pointsToUse.get(0);
                List<BigDecimal> endPoint = pointsToUse.get(pointsToUse.size() - 1);

                RouteSegmentResponse segment = new RouteSegmentResponse(
                    1,
                    formatPointLabel(typesToUse.get(0), 0),
                    formatPointLabel(typesToUse.get(typesToUse.size() - 1), typesToUse.size() - 1),
                    startPoint.get(1),  // startLat
                    startPoint.get(0),  // startLng
                    endPoint.get(1),    // endLat
                    endPoint.get(0),    // endLng
                    path,
                    tolls,
                    totalDistance,  // Add the segment distance
                    rawData
                );

                return new SuggestRouteResponse(List.of(segment), totalTollAmount, totalTollCount, totalDistance);
            }
        } catch (Exception ex) {
            log.warn("Error getting route from Vietmap: {}", ex.getMessage());
            // fallback: build mocked segments using ordered points
            List<RouteSegmentResponse> segments = new ArrayList<>();
            long totalToll = 0L;
            int totalTollCount = 0;
            double totalDistance = 0;

            for (int i = 0; i < pointsToUse.size() - 1; i++) {
                List<BigDecimal> start = pointsToUse.get(i);
                List<BigDecimal> end = pointsToUse.get(i + 1);
                List<List<BigDecimal>> path = Arrays.asList(
                        Arrays.asList(start.get(0), start.get(1)),
                        Arrays.asList(end.get(0), end.get(1))
                );
                List<TollResponse> emptyTolls = Collections.emptyList();

                // Add point type information to raw data
                Map<String, Object> rawData = new HashMap<>();
                rawData.put("mock", true);
                rawData.put("segmentIndex", i);
                rawData.put("vehicleTypeId", request.vehicleTypeId());
                if (typesToUse != null && typesToUse.size() > i) {
                    rawData.put("fromType", typesToUse.get(i));
                    if (i + 1 < typesToUse.size()) {
                        rawData.put("toType", typesToUse.get(i + 1));
                    }
                }

                // Calculate distance for this straight-line segment
                double segmentDistance = calculateDistance(start, end);
                totalDistance += segmentDistance;

                RouteSegmentResponse seg = new RouteSegmentResponse(
                        i + 1,
                        formatPointLabel(typesToUse.get(i), i),
                        formatPointLabel(typesToUse.get(i + 1), i + 1),
                        start.get(1),  // startLat
                        start.get(0),  // startLng
                        end.get(1),    // endLat
                        end.get(0),    // endLng
                        path,
                        emptyTolls,
                        segmentDistance, // Add the distance parameter
                        rawData
                );
                segments.add(seg);
            }
            return new SuggestRouteResponse(segments, totalToll, totalTollCount, totalDistance);
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

    /**
     * Fallback point order optimization using nearest-neighbor heuristic.
     * Given a list of points and their optional types, reorder the points to minimize travel distance.
     * Returns a new ordered list of points and corresponding types.
     */
    private PointOrderResult orderPointsNearestNeighbor(List<List<BigDecimal>> rawPoints, List<String> pointTypes) {
        if (rawPoints == null || rawPoints.size() < 2) {
            return new PointOrderResult(rawPoints, pointTypes);
        }

        try {
            // Start with the first point as the initial "current" point
            List<BigDecimal> startPoint = rawPoints.get(0);
            List<List<BigDecimal>> orderedPoints = new ArrayList<>();
            List<String> orderedTypes = new ArrayList<>();

            orderedPoints.add(startPoint);
            if (pointTypes != null && !pointTypes.isEmpty()) {
                orderedTypes.add(pointTypes.get(0));
            }

            Set<List<BigDecimal>> remainingPoints = new HashSet<>(rawPoints);
            remainingPoints.remove(startPoint);

            List<BigDecimal> currentPoint = startPoint;

            // While there are remaining points, find the nearest neighbor and add to the route
            while (!remainingPoints.isEmpty()) {
                List<BigDecimal> nearestPoint = null;
                double nearestDistance = Double.MAX_VALUE;

                for (List<BigDecimal> candidate : remainingPoints) {
                    double distance = calculateDistance(currentPoint, candidate);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearestPoint = candidate;
                    }
                }

                if (nearestPoint != null) {
                    orderedPoints.add(nearestPoint);
                    currentPoint = nearestPoint;
                    remainingPoints.remove(nearestPoint);

                    // Also reorder the types if provided
                    if (pointTypes != null && !pointTypes.isEmpty()) {
                        int index = rawPoints.indexOf(nearestPoint);
                        if (index >= 0 && index < pointTypes.size()) {
                            orderedTypes.add(pointTypes.get(index));
                        }
                    }
                }
            }

            return new PointOrderResult(orderedPoints, orderedTypes);
        } catch (Exception ex) {
            log.warn("Error optimizing point order: {}", ex.getMessage());
            // In case of error, fallback to original order
            return new PointOrderResult(rawPoints, pointTypes);
        }
    }

    /**
     * Calculate distance between two points (lng/lat) using Haversine formula.
     * Used for quick distance estimation in optimization algorithms.
     */
    private double calculateDistanceHaversine(List<BigDecimal> point1, List<BigDecimal> point2) {
        if (point1 == null || point2 == null || point1.size() < 2 || point2.size() < 2) {
            return 0.0;
        }

        final int R = 6371; // Radius of the earth in kilometers
        
        double lat1 = point1.get(1).doubleValue();
        double lon1 = point1.get(0).doubleValue();
        double lat2 = point2.get(1).doubleValue();
        double lon2 = point2.get(0).doubleValue();

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c; // Distance in kilometers
    }

    /**
     * Calculate distance between two points (lng/lat) using VietMap API.
     * Falls back to Haversine formula if VietMap fails.
     * Use this for final accurate distance calculation, not for optimization loops.
     */
    private double calculateDistanceVietMap(List<BigDecimal> point1, List<BigDecimal> point2) {
        if (point1 == null || point2 == null || point1.size() < 2 || point2.size() < 2) {
            return 0.0;
        }

        double lat1 = point1.get(1).doubleValue();
        double lon1 = point1.get(0).doubleValue();
        double lat2 = point2.get(1).doubleValue();
        double lon2 = point2.get(0).doubleValue();

        try {
            // Use VietMap API for accurate road distance
            BigDecimal distanceKm = vietMapDistanceService.calculateDistance(lat1, lon1, lat2, lon2, "car");
            return distanceKm.doubleValue();
        } catch (Exception e) {
            // Fallback to Haversine formula
            log.warn("VietMap distance calculation failed, using Haversine fallback: {}", e.getMessage());
            return calculateDistanceHaversine(point1, point2);
        }
    }

    /**
     * Calculate distance between two points - defaults to Haversine for performance.
     * Used in optimization algorithms where speed is more important than accuracy.
     */
    private double calculateDistance(List<BigDecimal> point1, List<BigDecimal> point2) {
        return calculateDistanceHaversine(point1, point2);
    }

    /**
     * Calculate the total distance along a path of coordinates using Haversine.
     * Used only for estimating distance along detailed path segments.
     * For accurate waypoint-to-waypoint distance, use calculateDistanceVietMap directly.
     * @param path List of coordinate points [lng, lat]
     * @return Total distance in kilometers
     */
    private double calculatePathDistance(List<List<BigDecimal>> path) {
        if (path == null || path.size() < 2) {
            return 0;
        }

        double totalDistance = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            // Use Haversine for quick estimation along detailed path
            totalDistance += calculateDistanceHaversine(path.get(i), path.get(i + 1));
        }

        return totalDistance;
    }

    /**
     * Helper class to encapsulate the result of point order optimization.
     */
    private static class PointOrderResult {
        List<List<BigDecimal>> points;
        List<String> types;

        PointOrderResult(List<List<BigDecimal>> points, List<String> types) {
            this.points = points;
            this.types = types;
        }
    }

    private String getPointLabel(List<String> types, int index, String defaultLabel) {
        if (types != null && types.size() > index) {
            String type = types.get(index);
            if (type != null && !type.isEmpty()) {
                return type;
            }
        }
        return defaultLabel;
    }

    /**
     * Generate default point types for the given number of points.
     * Used when the provided pointTypes is null or has different size than points.
     */
    private List<String> generateDefaultPointTypes(int count) {
        List<String> defaultTypes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            defaultTypes.add("Point " + (i + 1));
        }
        return defaultTypes;
    }

    /**
     * Create raw data map for response based on request and point types.
     */
    private Map<String, Object> createRawDataForResponse(SuggestRouteRequest request, List<String> typesToUse, boolean optimize) {
        Map<String, Object> rawData = new HashMap<>();
        rawData.put("source", "vietmap");
        if (optimize) {
            rawData.put("optimized", true);
        }
        if (typesToUse != null && !typesToUse.isEmpty()) {
            rawData.put("pointTypes", typesToUse);
        }
        return rawData;
    }

    /**
     * Extract a segment of the path between two waypoints.
     * Finds the closest points in the full path to the given waypoints and extracts that segment.
     */
    private List<List<BigDecimal>> extractPathSegment(List<List<BigDecimal>> fullPath, List<BigDecimal> startPoint, List<BigDecimal> endPoint) {
        if (fullPath == null || fullPath.isEmpty()) {
            return Collections.emptyList();
        }

        // Find the index of the point in fullPath closest to startPoint
        int startIndex = findClosestPointIndex(fullPath, startPoint);

        // Find the index of the point in fullPath closest to endPoint
        int endIndex = findClosestPointIndex(fullPath, endPoint);

        // Ensure proper ordering (start should come before end)
        if (startIndex > endIndex) {
            // If the path wraps around, we need to select the appropriate segment
            // For simplicity, we'll just return the shorter segment
            if (startIndex - endIndex < fullPath.size() - startIndex + endIndex) {
                return fullPath.subList(endIndex, startIndex + 1);
            } else {
                List<List<BigDecimal>> result = new ArrayList<>();
                result.addAll(fullPath.subList(startIndex, fullPath.size()));
                result.addAll(fullPath.subList(0, endIndex + 1));
                return result;
            }
        } else if (startIndex == endIndex) {
            // If they're the same point, just return that point
            return Collections.singletonList(fullPath.get(startIndex));
        } else {
            // Normal case: start comes before end
            return fullPath.subList(startIndex, endIndex + 1);
        }
    }

    /**
     * Find the index of the point in the list that is closest to the target point.
     */
    private int findClosestPointIndex(List<List<BigDecimal>> points, List<BigDecimal> target) {
        int closestIndex = 0;
        double closestDistance = Double.MAX_VALUE;

        for (int i = 0; i < points.size(); i++) {
            double distance = calculateDistance(points.get(i), target);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestIndex = i;
            }
        }

        return closestIndex;
    }

    /**
     * Distribute tolls across multiple segments.
     * For simplicity, this example distributes tolls evenly based on the number of segments.
     */
    private void distributeTollsToSegments(List<RouteSegmentResponse> segments, List<TollResponse> tolls, long totalToll) {
        if (segments == null || segments.isEmpty() || tolls == null || tolls.isEmpty()) {
            return;
        }

        int numSegments = segments.size();
        int numTolls = tolls.size();

        // Since RouteSegmentResponse is immutable (a record), we need to create new instances
        // Simple case: if only one segment, assign all tolls to it
        if (numSegments == 1) {
            RouteSegmentResponse oldSegment = segments.get(0);
            RouteSegmentResponse newSegment = new RouteSegmentResponse(
                oldSegment.segmentOrder(),
                oldSegment.startName(),
                oldSegment.endName(),
                oldSegment.startLat(),
                oldSegment.startLng(),
                oldSegment.endLat(),
                oldSegment.endLng(),
                oldSegment.path(),
                tolls, // Assign all tolls to this segment
                oldSegment.distance(), // Keep the original distance
                oldSegment.rawResponse()
            );
            segments.set(0, newSegment);
            return;
        }

        // Distribute tolls across segments
        // First, prepare toll lists for each segment
        List<List<TollResponse>> segmentTolls = new ArrayList<>();
        for (int i = 0; i < numSegments; i++) {
            segmentTolls.add(new ArrayList<>());
        }

        // Assign tolls to segments using round-robin
        for (int i = 0; i < numTolls; i++) {
            TollResponse toll = tolls.get(i);
            int segmentIndex = i % numSegments;
            segmentTolls.get(segmentIndex).add(toll);
        }

        // Create new segment responses with the distributed tolls
        for (int i = 0; i < numSegments; i++) {
            RouteSegmentResponse oldSegment = segments.get(i);
            RouteSegmentResponse newSegment = new RouteSegmentResponse(
                oldSegment.segmentOrder(),
                oldSegment.startName(),
                oldSegment.endName(),
                oldSegment.startLat(),
                oldSegment.startLng(),
                oldSegment.endLat(),
                oldSegment.endLng(),
                oldSegment.path(),
                segmentTolls.get(i),
                oldSegment.distance(), // Keep the original distance
                oldSegment.rawResponse()
            );
            segments.set(i, newSegment);
        }
    }

    /**
     * Format a user-friendly label for a point based on its type and index
     */
    private String formatPointLabel(String pointType, int index) {
        if (pointType == null || pointType.isEmpty()) {
            return "Point " + (index + 1);
        }

        // Capitalize first letter
        String capitalizedType = pointType.substring(0, 1).toUpperCase() + pointType.substring(1).toLowerCase();
        return capitalizedType;
    }
}
