package capstone_project.config.admin;

import capstone_project.entity.RolesEntity;
import capstone_project.entity.UsersEntity;
import capstone_project.enums.RoleType;
import capstone_project.service.entityServices.RolesEntityService;
import capstone_project.service.entityServices.UsersEntityService;
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

    private final UsersEntityService usersEntityService;
    private final RolesEntityService rolesEntityService;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @PostConstruct
    public void init() {
        String username = adminProperties.getUsername();
        String email = adminProperties.getEmail();
        String password = adminProperties.getPassword();

        Optional<UsersEntity> existing = usersEntityService.getUserByUserName(username);
        if (existing.isEmpty()) {
            RolesEntity adminRole = rolesEntityService.findByRoleName(RoleType.ADMIN.name())
                    .orElseGet(() -> {
                        RolesEntity role = RolesEntity.builder()
                                .roleName(RoleType.ADMIN.name())
                                .description("Default Admin Role")
                                .isActive(true)
                                .build();
                        return rolesEntityService.save(role);
                    });

            UsersEntity admin = UsersEntity.builder()
                    .username(username)
                    .password(passwordEncoder.encode(password))
                    .email(email)
                    .fullName("Administrator")
                    .status("active")
                    .dateOfBirth(LocalDate.of(2000, 1, 1))
                    .createdAt(LocalDateTime.now())
                    .role(adminRole)
                    .build();

            usersEntityService.save(admin);
            System.out.println("Default admin account created.");
        } else {
            System.out.println("Admin account already exists.");
        }
    }
}