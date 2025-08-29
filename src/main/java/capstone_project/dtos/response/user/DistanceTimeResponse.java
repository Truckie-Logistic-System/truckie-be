package capstone_project.dtos.response.user;

public record DistanceTimeResponse(
        double distance,
        double time
) {
    private static final double METERS_TO_KM = 0.001;
    private static final double MS_TO_HOURS = 1.0 / (1000 * 60 * 60);

    public static DistanceTimeResponse fromRouteResponse(RouteResponse routeResponse) {
        if (routeResponse.paths() == null || routeResponse.paths().isEmpty()) {
            return new DistanceTimeResponse(0, 0);
        }
        RouteResponse.Path path = routeResponse.paths().get(0);
        return new DistanceTimeResponse(
                path.distance() * METERS_TO_KM,
                path.time() * MS_TO_HOURS
        );
    }

    public double getDistanceInKm() {
        return distance;
    }

    public double getTimeInHours() {
        return time;
    }

    public double getDistanceInMeters() {
        return distance * 1000;
    }

    public long getTimeInSeconds() {
        return (long) (time * 3600);
    }

    public long getTimeInMinutes() {
        return (long) (time * 60);
    }
}