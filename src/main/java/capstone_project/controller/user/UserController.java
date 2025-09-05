package capstone_project.controller.user;

import capstone_project.dtos.request.user.UpdateUserRequest;
import capstone_project.dtos.response.auth.UserResponse;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.service.services.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${user.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final UserService userService;

    @GetMapping("/{username}/username")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByUserName(@PathVariable String username) {
        final var userByUserName = userService.getUserByUserName(username);
        return ResponseEntity.ok(ApiResponse.ok(userByUserName));
    }

    @GetMapping("/{email}/email")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByEmail(@PathVariable String email) {
        final var userByEmail = userService.getUserByEmail(email);
        return ResponseEntity.ok(ApiResponse.ok(userByEmail));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByUserName(@PathVariable UUID id) {
        final var userById = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.ok(userById));
    }

    @GetMapping("")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        final var userById = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.ok(userById));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUserByUserNameOrEmailLike(@RequestParam(required = false) final String username, @RequestParam(required = false) final String email) {
        final var users = userService.getUserByUserNameOrEmailLike(username, email);
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @PutMapping("/status/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatusById(@PathVariable UUID id, @RequestParam String status) {
        final var user = userService.updateUserStatusById(id, status);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatusById(@PathVariable UUID id, @RequestBody UpdateUserRequest request) {
        final var user = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }
}
