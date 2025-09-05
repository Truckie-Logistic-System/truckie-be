package capstone_project.config.admin;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.entity.auth.RoleEntity;
import capstone_project.entity.auth.UserEntity;
import capstone_project.common.enums.RoleTypeEnum;
import capstone_project.repository.entityServices.auth.RoleEntityService;
import capstone_project.repository.entityServices.auth.UserEntityService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final UserEntityService userEntityService;
    private final RoleEntityService roleEntityService;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @PostConstruct
    public void init() {
        String username = adminProperties.getUsername();
        String email = adminProperties.getEmail();
        String password = adminProperties.getPassword();

        Optional<UserEntity> existing = userEntityService.getUserByUserName(username);
        if (existing.isEmpty()) {
            RoleEntity adminRole = roleEntityService.findByRoleName(RoleTypeEnum.ADMIN.name())
                    .orElseGet(() -> {
                        RoleEntity role = RoleEntity.builder()
                                .roleName(RoleTypeEnum.ADMIN.name())
                                .description("Default Admin Role")
                                .isActive(true)
                                .build();
                        return roleEntityService.save(role);
                    });

            UserEntity admin = UserEntity.builder()
                    .username(username)
                    .password(passwordEncoder.encode(password))
                    .email(email)
                    .fullName("Administrator")
                    .status(CommonStatusEnum.ACTIVE.name())
                    .dateOfBirth(LocalDate.of(2000, 1, 1))
                    .createdAt(LocalDateTime.now())
                    .role(adminRole)
                    .build();

            userEntityService.save(admin);
            System.out.println("Default admin account created.");
        } else {
            System.out.println("Admin account already exists.");
        }
    }
}