package capstone_project.service.services.user.impl;

import capstone_project.dtos.response.user.DuplicateUserCleanupResponse;
import capstone_project.dtos.response.user.DuplicateUserCleanupResponse.DeletedUserInfo;
import capstone_project.entity.auth.UserEntity;
import capstone_project.repository.repositories.auth.UserRepository;
import capstone_project.service.services.user.UserCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementation of UserCleanupService
 * Handles cleanup of duplicate users (same username) by renaming one of them
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserCleanupServiceImpl implements UserCleanupService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    // Track used usernames to avoid conflicts
    private Set<String> usedUsernames;
    private Set<String> usedFullNames;
    
    // Vietnamese names for random generation
    private static final String[] FAMILY_NAMES = {
            "Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng",
            "Bùi", "Đỗ", "Hồ", "Ngô", "Dương", "Lý", "Đinh", "Mai", "Trương", "Lương"
    };
    
    private static final String[] MIDDLE_NAMES = {
            "Văn", "Thị", "Hữu", "Đức", "Minh", "Thanh", "Quốc", "Xuân", "Kim", "Ngọc",
            "Hoàng", "Phúc", "Anh", "Tuấn", "Thành", "Hồng", "Quang", "Thiên", "Bảo", "Như"
    };
    
    private static final String[] FIRST_NAMES = {
            "An", "Bình", "Cường", "Dũng", "Em", "Phúc", "Giang", "Hải", "Kiên", "Long",
            "Minh", "Nam", "Phong", "Quang", "Sơn", "Tâm", "Tuấn", "Việt", "Xuân", "Yên",
            "Anh", "Bảo", "Chi", "Duy", "Hà", "Hằng", "Hoa", "Hương", "Lan", "Linh",
            "Mai", "My", "Nga", "Nhung", "Oanh", "Phương", "Thảo", "Thu", "Trang", "Uyên",
            "Đạt", "Hiếu", "Hùng", "Khoa", "Lộc", "Nghĩa", "Tài", "Thắng", "Trung", "Vinh"
    };
    
    private final Random random = new Random();

    @Override
    @Transactional
    public DuplicateUserCleanupResponse cleanupDuplicateUsers(boolean dryRun) {
        log.info("🔍 Starting duplicate user cleanup (rename mode). DryRun mode: {}", dryRun);
        
        List<DeletedUserInfo> modifiedUsers = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        // Initialize used usernames and fullNames set
        List<UserEntity> allUsersInDb = userRepository.findAll();
        usedUsernames = allUsersInDb.stream()
                .map(UserEntity::getUsername)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        usedFullNames = allUsersInDb.stream()
                .map(UserEntity::getFullName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        // Step 1: Get all users and group by username (exclude ADMIN)
        List<UserEntity> allUsers = allUsersInDb.stream()
                .filter(u -> u.getRole() != null && !"ADMIN".equalsIgnoreCase(u.getRole().getRoleName()))
                .collect(Collectors.toList());
        
        Map<String, List<UserEntity>> usersByUsername = allUsers.stream()
                .filter(u -> u.getUsername() != null && !u.getUsername().isEmpty())
                .collect(Collectors.groupingBy(UserEntity::getUsername));
        
        // Step 2: Find duplicate groups (more than 1 user with same username)
        Map<String, List<UserEntity>> duplicateGroups = usersByUsername.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        log.info("📊 Found {} usernames with duplicates", duplicateGroups.size());
        
        // Step 3: Process each duplicate group
        for (Map.Entry<String, List<UserEntity>> entry : duplicateGroups.entrySet()) {
            String username = entry.getKey();
            List<UserEntity> duplicates = entry.getValue();
            
            // Sort by createdAt to keep the oldest (first created) user unchanged
            duplicates.sort(Comparator.comparing(
                    u -> u.getCreatedAt() != null ? u.getCreatedAt() : java.time.LocalDateTime.MAX
            ));
            
            log.info("🔄 Processing username '{}' with {} duplicates", username, duplicates.size());
            
            // Keep the first (oldest) user, rename the rest
            for (int i = 1; i < duplicates.size(); i++) {
                UserEntity userToModify = duplicates.get(i);
                
                try {
                    String roleName = userToModify.getRole() != null ? userToModify.getRole().getRoleName() : "UNKNOWN";
                    
                    // Skip ADMIN users
                    if ("ADMIN".equalsIgnoreCase(roleName)) {
                        log.info("⏭️ Skipping ADMIN user: {}", userToModify.getId());
                        continue;
                    }
                    
                    String oldUsername = userToModify.getUsername();
                    String oldEmail = userToModify.getEmail();
                    String oldFullName = userToModify.getFullName();
                    
                    // Generate new unique Vietnamese name and username
                    NameAndUsername generated = generateUniqueNameAndUsername(roleName);
                    
                    String newFullName = generated.fullName;
                    String newUsername = generated.username;
                    String newEmail = newUsername + "@gmail.com";
                    String newPassword = getPasswordByRole(roleName);
                    
                    log.info("📝 Modifying user {} - Old: {}|{}|{} -> New: {}|{}|{}", 
                            userToModify.getId(), oldFullName, oldUsername, oldEmail, 
                            newFullName, newUsername, newEmail);
                    
                    if (!dryRun) {
                        userToModify.setFullName(newFullName);
                        userToModify.setUsername(newUsername);
                        userToModify.setEmail(newEmail);
                        userToModify.setPassword(passwordEncoder.encode(newPassword));
                        userRepository.save(userToModify);
                    }
                    
                    // Mark as used
                    usedUsernames.add(newUsername);
                    usedFullNames.add(newFullName);
                    
                    modifiedUsers.add(DeletedUserInfo.builder()
                            .userId(userToModify.getId())
                            .username(oldUsername + " -> " + newUsername)
                            .email(oldEmail + " -> " + newEmail)
                            .roleName(roleName)
                            .hadCustomerRecord(false)
                            .hadDriverRecord(false)
                            .build());
                    
                } catch (Exception e) {
                    String errorMsg = String.format("Failed to modify user %s (username: %s): %s", 
                            userToModify.getId(), userToModify.getUsername(), e.getMessage());
                    log.error("❌ {}", errorMsg, e);
                    errors.add(errorMsg);
                }
            }
        }
        
        log.info("✅ Duplicate user cleanup {}. Groups: {}, Users modified: {}", 
                dryRun ? "preview completed" : "completed",
                duplicateGroups.size(), 
                modifiedUsers.size());
        
        return DuplicateUserCleanupResponse.builder()
                .totalDuplicateGroupsFound(duplicateGroups.size())
                .totalUsersDeleted(modifiedUsers.size()) // Actually "modified" not deleted
                .totalCustomersDeleted(0)
                .totalDriversDeleted(0)
                .deletedUsers(modifiedUsers) // Actually "modifiedUsers"
                .errors(errors)
                .build();
    }
    
    /**
     * Container for generated name and username
     */
    private static class NameAndUsername {
        String fullName;
        String username;
        
        NameAndUsername(String fullName, String username) {
            this.fullName = fullName;
            this.username = username;
        }
    }
    
    /**
     * Generate a unique Vietnamese name and corresponding username
     * Keeps trying until finding one that doesn't conflict
     */
    private NameAndUsername generateUniqueNameAndUsername(String roleName) {
        int maxAttempts = 1000;
        int attempts = 0;
        
        while (attempts < maxAttempts) {
            attempts++;
            
            // Generate random Vietnamese name
            String familyName = FAMILY_NAMES[random.nextInt(FAMILY_NAMES.length)];
            String middleName = MIDDLE_NAMES[random.nextInt(MIDDLE_NAMES.length)];
            String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
            
            String fullName = familyName + " " + middleName + " " + firstName;
            
            // Generate username from this name
            String baseUsername = generateUsernameFromFullName(fullName);
            String finalUsername = applyRolePrefix(baseUsername, roleName);
            
            // Check if both are unique
            if (!usedUsernames.contains(finalUsername) && !usedFullNames.contains(fullName)) {
                return new NameAndUsername(fullName, finalUsername);
            }
        }
        
        // Fallback: use UUID-based name (should rarely happen with 20x20x50 = 20000 combinations)
        String uuid = UUID.randomUUID().toString().substring(0, 6);
        String fallbackName = "Người Dùng " + uuid;
        String fallbackUsername = applyRolePrefix("nguoidung" + uuid, roleName);
        return new NameAndUsername(fallbackName, fallbackUsername);
    }
    
    /**
     * Generate username from full name
     * Example: "Nguyễn Hữu Tài" -> "tainh" (Tài + Nguyễn + Hữu initials)
     */
    private String generateUsernameFromFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "user" + UUID.randomUUID().toString().substring(0, 8);
        }
        
        // Normalize and remove diacritics
        String normalized = removeDiacritics(fullName.trim().toLowerCase());
        
        // Split into parts
        String[] parts = normalized.split("\\s+");
        
        if (parts.length == 0) {
            return "user" + UUID.randomUUID().toString().substring(0, 8);
        }
        
        if (parts.length == 1) {
            return parts[0];
        }
        
        // Last part is the first name (Vietnamese naming convention)
        // First part is family name, middle parts are middle names
        // Pattern: firstName + initials of other names
        // "nguyen huu tai" -> "tai" + "n" + "h" = "tainh"
        
        String firstName = parts[parts.length - 1]; // Last word is first name
        StringBuilder initials = new StringBuilder();
        
        for (int i = 0; i < parts.length - 1; i++) {
            if (!parts[i].isEmpty()) {
                initials.append(parts[i].charAt(0));
            }
        }
        
        return firstName + initials.toString();
    }
    
    /**
     * Remove Vietnamese diacritics from string
     */
    private String removeDiacritics(String input) {
        if (input == null) return null;
        
        // Special Vietnamese characters mapping
        String result = input
                .replace("đ", "d")
                .replace("Đ", "D");
        
        // Normalize and remove combining diacritical marks
        String normalized = Normalizer.normalize(result, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("");
    }
    
    /**
     * Apply role prefix to username
     */
    private String applyRolePrefix(String baseUsername, String roleName) {
        if (roleName == null) return baseUsername;
        
        return switch (roleName.toUpperCase()) {
            case "STAFF" -> "staff" + baseUsername;
            case "DRIVER" -> "driver" + baseUsername;
            case "CUSTOMER" -> baseUsername; // No prefix for customer
            default -> baseUsername;
        };
    }
    
    /**
     * Get default password by role
     */
    private String getPasswordByRole(String roleName) {
        if (roleName == null) return "password123";
        
        return switch (roleName.toUpperCase()) {
            case "STAFF" -> "staff";
            case "DRIVER" -> "driver";
            case "CUSTOMER" -> "customer";
            default -> "password123";
        };
    }
}
