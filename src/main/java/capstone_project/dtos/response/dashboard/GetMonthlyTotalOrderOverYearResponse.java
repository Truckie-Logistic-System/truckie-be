package capstone_project.dtos.response.dashboard;

import java.util.Map;

public record GetMonthlyTotalOrderOverYearResponse(

        boolean success,
        String message,
        Map<String, Integer> data
) {
}
