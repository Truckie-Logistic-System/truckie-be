package capstone_project.service.auth;

import capstone_project.entity.auth.UserEntity;
import capstone_project.repository.repositories.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthUserService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Try to find by username first
        List<UserEntity> usersByUsername = userRepository.findAllByUsernameWithRole(username);
        
        UserEntity user = null;
        
        if (!usersByUsername.isEmpty()) {
            if (usersByUsername.size() > 1) {
                log.warn("⚠️ Found {} users with username {}. Using first ACTIVE user.", 
                        usersByUsername.size(), username);
                // Prefer ACTIVE user
                user = usersByUsername.stream()
                        .filter(u -> "ACTIVE".equals(u.getStatus()))
                        .findFirst()
                        .orElse(usersByUsername.get(0));
            } else {
                user = usersByUsername.get(0);
            }
        }
        
        // If not found by username, try email
        if (user == null) {
            user = userRepository.findFirstByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username or email: " + username));
        }

        String password = user.getPassword();
        if (password == null || password.isEmpty()) {
            password = "NO_PASSWORD";
        }

        Collection<? extends GrantedAuthority> authorities = getAuthorities(user);
        if (authorities == null || authorities.isEmpty()) {
            throw new IllegalArgumentException("Authorities cannot be null or empty");
        }

        return new User(
                user.getUsername(),
                password,
                authorities
        );
    }

    private Collection<? extends GrantedAuthority> getAuthorities(UserEntity user) {
        // Spring Security expects authorities with ROLE_ prefix for hasRole() checks
        // But we use hasAuthority() so we add ROLE_ prefix for consistency
        String roleName = user.getRole().getRoleName();
        return List.of(
            new SimpleGrantedAuthority("ROLE_" + roleName),  // For hasRole() checks
            new SimpleGrantedAuthority(roleName)              // For hasAuthority() checks
        );
    }
}