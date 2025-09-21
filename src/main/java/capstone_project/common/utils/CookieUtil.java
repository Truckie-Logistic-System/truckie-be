package capstone_project.common.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    @Value("${auth.cookie.domain:localhost}")
    private String cookieDomain;

    @Value("${auth.cookie.secure:false}")
    private boolean secure;

    @Value("${auth.cookie.access-token-expiry:3600}")
    private int accessTokenExpiry; // in seconds

    @Value("${auth.cookie.refresh-token-expiry:2592000}")
    private int refreshTokenExpiry; // in seconds, default 30 days

    /**
     * Create an HTTP-only secure cookie for the access token
     */
    public void createAccessTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("access_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setMaxAge(accessTokenExpiry);
        cookie.setPath("/");
        if (!cookieDomain.equals("localhost")) {
            cookie.setDomain(cookieDomain);
        }
        response.addCookie(cookie);
    }

    /**
     * Create an HTTP-only secure cookie for the refresh token
     */
    public void createRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refresh_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setMaxAge(refreshTokenExpiry);
        cookie.setPath("/");
        if (!cookieDomain.equals("localhost")) {
            cookie.setDomain(cookieDomain);
        }
        response.addCookie(cookie);
    }

    /**
     * Clear all authentication cookies
     */
    public void clearAuthCookies(HttpServletResponse response) {
        Cookie accessTokenCookie = new Cookie("access_token", "");
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(secure);
        accessTokenCookie.setMaxAge(0);
        accessTokenCookie.setPath("/");

        Cookie refreshTokenCookie = new Cookie("refresh_token", "");
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(secure);
        refreshTokenCookie.setMaxAge(0);
        refreshTokenCookie.setPath("/");

        if (!cookieDomain.equals("localhost")) {
            accessTokenCookie.setDomain(cookieDomain);
            refreshTokenCookie.setDomain(cookieDomain);
        }

        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);
    }

    /**
     * Get a specific cookie by name
     */
    public String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
