package capstone_project.service.auth;


import capstone_project.entity.UsersEntity;
import capstone_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
public class AuthUserService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        final var user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username or email: " + username));

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

    private Collection<? extends GrantedAuthority> getAuthorities(UsersEntity user) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getRoleName()));
    }
}