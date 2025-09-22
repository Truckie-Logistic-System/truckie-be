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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserContextUtils {
    private final UserEntityService userEntityService;
    private final CustomerEntityService customerEntityService;
    private final DriverEntityService driverEntityService;

    /**
     * Lấy tên người dùng hiện tại từ SecurityContextHolder
     * @return tên người dùng đã xác thực
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage() + ", No authenticated user found",
                    ErrorEnum.NOT_FOUND.getErrorCode());
        }
        return authentication.getName();
    }

    /**
     * Lấy đối tượng UserEntity của người dùng hiện tại
     * @return đối tượng UserEntity
     */
    public UserEntity getCurrentUser() {
        String username = getCurrentUsername();
        return userEntityService.getUserByUserName(username)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + ", User not found with username: " + username,
                        ErrorEnum.NOT_FOUND.getErrorCode()));
    }

    /**
     * Lấy ID của người dùng hiện tại
     * @return UUID của người dùng
     */
    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /**
     * Lấy đối tượng CustomerEntity của người dùng hiện tại
     * @return đối tượng CustomerEntity
     */
    public CustomerEntity getCurrentCustomer() {
        UUID userId = getCurrentUserId();
        return customerEntityService.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + ", Customer not found for current user",
                        ErrorEnum.NOT_FOUND.getErrorCode()));
    }

    /**
     * Lấy ID của khách hàng hiện tại
     * @return UUID của khách hàng
     */
    public UUID getCurrentCustomerId() {
        return getCurrentCustomer().getId();
    }

    /**
     * Lấy đối tượng DriverEntity của người dùng hiện tại
     * @return đối tượng DriverEntity
     */
    public DriverEntity getCurrentDriver() {
        UUID userId = getCurrentUserId();
        return driverEntityService.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + ", Driver not found for current user",
                        ErrorEnum.NOT_FOUND.getErrorCode()));
    }

    /**
     * Lấy ID của tài xế hiện tại
     * @return UUID của tài xế
     */
    public UUID getCurrentDriverId() {
        return getCurrentDriver().getId();
    }
}