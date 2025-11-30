package capstone_project.service.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serializable wrapper for UserDetails
 * Used for Redis caching to avoid Jackson serialization issues with Spring Security's User class
 */
public class CachedUserDetails implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String username;
    private String password;
    private List<String> authorities;
    private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private boolean enabled;
    
    /**
     * Default constructor
     */
    public CachedUserDetails() {
        this.authorities = new ArrayList<>();
        this.accountNonExpired = true;
        this.accountNonLocked = true;
        this.credentialsNonExpired = true;
        this.enabled = true;
    }
    
    /**
     * Create from Spring Security UserDetails
     */
    public static CachedUserDetails from(UserDetails userDetails) {
        CachedUserDetails cached = new CachedUserDetails();
        cached.username = userDetails.getUsername();
        cached.password = userDetails.getPassword();
        cached.authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        cached.accountNonExpired = userDetails.isAccountNonExpired();
        cached.accountNonLocked = userDetails.isAccountNonLocked();
        cached.credentialsNonExpired = userDetails.isCredentialsNonExpired();
        cached.enabled = userDetails.isEnabled();
        return cached;
    }
    
    /**
     * Convert to Spring Security UserDetails
     */
    public UserDetails toUserDetails() {
        List<GrantedAuthority> grantedAuthorities = authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        
        return new User(
                username,
                password,
                enabled,
                accountNonExpired,
                credentialsNonExpired,
                accountNonLocked,
                grantedAuthorities
        );
    }
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
    }

    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    public void setAccountNonExpired(boolean accountNonExpired) {
        this.accountNonExpired = accountNonExpired;
    }

    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    public void setAccountNonLocked(boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }

    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    public void setCredentialsNonExpired(boolean credentialsNonExpired) {
        this.credentialsNonExpired = credentialsNonExpired;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
