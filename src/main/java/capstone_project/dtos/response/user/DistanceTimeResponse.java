package capstone_project.dtos.response.user;

public record DistanceTimeResponse(
        double distance,  // in meters
        long time         // in seconds
) {
    public static DistanceTimeResponse fromRouteResponse(RouteResponse routeResponse) {
        if (routeResponse.paths() == null || routeResponse.paths().isEmpty()) {
            return new DistanceTimeResponse(0, 0);
        }
        RouteResponse.Path path = routeResponse.paths().get(0);
        return new DistanceTimeResponse(
                path.distance(),
                path.time() / 1000  // convert ms to seconds
        );
    }
}