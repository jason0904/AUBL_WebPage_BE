package com.aubl.webpage.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Firebase ID 토큰을 검증해 SecurityContext에 Authentication을 설정하는 필터.
 * admin 커스텀 클레임이 true일 때 ROLE_ADMIN을 부여한다.
 */
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(token);
            Collection<GrantedAuthority> authorities = resolveAuthorities(decoded);
            Authentication authentication = new FirebaseUserAuthentication(decoded.getUid(), authorities, decoded);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (FirebaseAuthException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Firebase token");
            return;
        }
        chain.doFilter(request, response);
    }

    private Collection<GrantedAuthority> resolveAuthorities(FirebaseToken token) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        Object adminClaim = token.getClaims().get("admin");
        if (adminClaim instanceof Boolean admin && Boolean.TRUE.equals(admin)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        // 기본 사용자 역할
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        return authorities;
    }

    private static class FirebaseUserAuthentication extends AbstractAuthenticationToken {
        private final String uid;
        private final FirebaseToken token;

        FirebaseUserAuthentication(String uid, Collection<? extends GrantedAuthority> authorities, FirebaseToken token) {
            super(authorities);
            this.uid = uid;
            this.token = token;
            setAuthenticated(true);
        }

        @Override
        public Object getCredentials() {
            return token;
        }

        @Override
        public Object getPrincipal() {
            return uid;
        }
    }
}
