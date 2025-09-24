package capstone_project.dtos.response.auth;

public class LoginPublicResponse {
    private final String userId;
    private final String username;
    private final String email;
    private final String roleName;

    public LoginPublicResponse(String userId, String username, String email, String roleName) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.roleName = roleName;
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRoleName() { return roleName; }
}