package capstone_project.dtos.request.auth;

public record ChangePasswordForForgetPassRequest(
        String username,
        String email,
        String newPassword,
        String confirmNewPassword
) {
}
