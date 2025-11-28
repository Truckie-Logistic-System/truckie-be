package capstone_project.dto.request.fcm;

import capstone_project.entity.fcm.FCMTokenEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterFCMTokenRequest {

    @NotBlank(message = "FCM token is required")
    private String token;

    @NotNull(message = "Device type is required")
    private FCMTokenEntity.DeviceType deviceType;

    private String deviceInfo;

    /**
     * Optional device information for debugging
     * Examples: "Samsung Galaxy S21 - Android 12", "iPhone 13 - iOS 15.0"
     */
    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }
}
