package capstone_project.common.utils;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.repository.entityServices.auth.UserEntityService;
import capstone_project.repository.entityServices.user.CustomerEntityService;
import capstone_project.repository.entityServices.user.DriverEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserContextUtils {
    private final UserEntityService userEntityService;
    private final CustomerEntityService customerEntityService;
    private final DriverEntityService driverEntityService;

    private static final String ANONYMOUS_USERNAME = "anonymousUser";

    /**
     * Lấy tên người dùng hiện tại từ SecurityContextHolder
     * - loại trừ AnonymousAuthenticationToken
     * - xử lý trường hợp gọi từ thread không có security context
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage() + ", No authenticated user found (SecurityContext is empty)",
                    ErrorEnum.NOT_FOUND.getErrorCode());
        }

        if (!authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage() + ", No authenticated user found (anonymous or not authenticated)",
                    ErrorEnum.NOT_FOUND.getErrorCode());
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }

        if (principal instanceof String) {
            String username = (String) principal;
            if (ANONYMOUS_USERNAME.equals(username)) {
                throw new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + ", No authenticated user found (anonymousUser)",
                        ErrorEnum.NOT_FOUND.getErrorCode());
            }
            return username;
        }

        throw new NotFoundException(
                ErrorEnum.NOT_FOUND.getMessage() + ", Unsupported principal type: " + (principal != null ? principal.getClass() : "null"),
                ErrorEnum.NOT_FOUND.getErrorCode());
    }

    public UserEntity getCurrentUser() {
        String username = getCurrentUsername();
        return userEntityService.getUserByUserName(username)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + ", User not found with username: " + username,
                        ErrorEnum.NOT_FOUND.getErrorCode()));
    }

    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public CustomerEntity getCurrentCustomer() {
        UUID userId = getCurrentUserId();
        return customerEntityService.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + ", Customer not found for current user",
                        ErrorEnum.NOT_FOUND.getErrorCode()));
    }

    public UUID getCurrentCustomerId() {
        return getCurrentCustomer().getId();
    }

    public DriverEntity getCurrentDriver() {
        UUID userId = getCurrentUserId();
        return driverEntityService.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + ", Driver not found for current user",
                        ErrorEnum.NOT_FOUND.getErrorCode()));
    }

    public UUID getCurrentDriverId() {
        return getCurrentDriver().getId();
    }
}
