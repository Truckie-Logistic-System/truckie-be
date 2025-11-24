package capstone_project.dtos.request.chat;

import jakarta.validation.constraints.NotBlank;

public record AIPersonalityRequest(
        @NotBlank(message = "User ID không được để trống")
        String userId,

        @NotBlank(message = "Personality không được để trống")
        String personality // PROFESSIONAL, FRIENDLY, EXPERT, QUICK
) {
}
