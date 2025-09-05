package capstone_project.dtos.request.user;

import java.time.LocalDate;

public record UpdateUserRequest(
        String fullName,
        String email,
        String phoneNumber,
        String gender,
        LocalDate dateOfBirth,
        String imageUrl
) {
}
