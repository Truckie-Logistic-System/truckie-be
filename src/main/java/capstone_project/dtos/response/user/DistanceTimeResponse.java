package capstone_project.dtos.response.user;

import java.math.BigDecimal;

public record DistanceTimeResponse(
        BigDecimal distance,
        double time
) {
    private static final double METERS_TO_KM = 0.001;
    private static final double MS_TO_HOURS = 1.0 / (1000 * 60 * 60);

    public static DistanceTimeResponse fromRouteResponse(RouteResponse routeResponse) {
        if (routeResponse.paths() == null || routeResponse.paths().isEmpty()) {
            return new DistanceTimeResponse(BigDecimal.ZERO, 0.0);
        }
        RouteResponse.Path path = routeResponse.paths().get(0);
        return new DistanceTimeResponse(
                BigDecimal.valueOf(path.distance() * METERS_TO_KM),
                path.time() * MS_TO_HOURS
        );
    }

    public BigDecimal getDistanceInKm() {
        return distance;
    }

    public double getTimeInHours() {
        return time;
    }

    public BigDecimal getDistanceInMeters() {
        return distance.multiply(BigDecimal.valueOf(1000));
    }

    public long getTimeInSeconds() {
        return (long) (time * 3600);
    }

    public long getTimeInMinutes() {
        return (long) (time * 60);
    }
}