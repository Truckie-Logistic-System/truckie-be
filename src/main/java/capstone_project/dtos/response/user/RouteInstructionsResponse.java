// src/main/java/capstone_project/dtos/response/user/RouteInstructionsResponse.java
package capstone_project.dtos.response.user;

import java.util.List;

public record RouteInstructionsResponse(
        List<Instruction> instructions
) {
    public record Instruction(
            String text,
            double distance,  // in meters
            long time,        // in seconds
            String streetName
    ) {}

    public static RouteInstructionsResponse fromRouteResponse(RouteResponse routeResponse) {
        if (routeResponse.paths() == null || routeResponse.paths().isEmpty()) {
            return new RouteInstructionsResponse(List.of());
        }

        List<Instruction> instructions = routeResponse.paths().get(0).instructions().stream()
                .map(i -> new Instruction(
                        i.text(),
                        i.distance(),
                        i.time() / 1000,  // convert ms to seconds
                        i.streetName()
                ))
                .toList();

        return new RouteInstructionsResponse(instructions);
    }
}