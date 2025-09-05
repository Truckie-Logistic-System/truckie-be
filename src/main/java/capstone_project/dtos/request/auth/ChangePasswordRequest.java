package capstone_project.dtos.request.auth;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(

        @NotBlank(message = "Username must not be blank")
        String username,

        @NotBlank(message = "Old password must not be blank")
        String oldPassword,

        @NotBlank(message = "New password must not be blank")
        String newPassword,

        @NotBlank(message = "Confirm new password must not be blank")
        String confirmNewPassword
) {
}
