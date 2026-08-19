package com.iotmining.services.dmss.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LiteJwtAuthenticationFilter")
class LiteJwtAuthenticationFilterTest {

    private static final String SECRET_B64 = "Vlo2vcFdiXGgWqZLEpLw6kk99sH8/4odgC2XgZV0IbA=";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_B64));

    private LiteJwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new LiteJwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "secretKeyBase64", SECRET_B64);
        request = new MockHttpServletRequest("GET", "/api/dashboards/list");
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private String token(Map<String, Object> claims) {
        return Jwts.builder()
                .claims(claims)
                .subject("test-user")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(KEY)
                .compact();
    }

    @Test
    @DisplayName("continues the chain unauthenticated when no Authorization header is present")
    void skipsWithoutAuthorizationHeader() throws Exception {
        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("authenticates and maps roles for a token with a populated role array")
    void authenticatesWithRoles() throws Exception {
        request.addHeader("Authorization", "Bearer " + token(Map.of(
                "tenantId", "tenant-1", "role", List.of("ROLE_ADMIN", "ROLE_USER"))));

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    @DisplayName("still authenticates (with no authorities) when the role claim is an empty array")
    void authenticatesWithEmptyRoleArray() throws Exception {
        // Regression test: claims.get("role", List.class) throws for some JJWT
        // internal representations of an empty array, which silently dropped
        // the entire authentication attempt (not just the roles) before this fix.
        request.addHeader("Authorization", "Bearer " + token(Map.of(
                "tenantId", "tenant-1", "role", List.of())));

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("authenticates (with no authorities) when the role claim is absent entirely")
    void authenticatesWithoutRoleClaim() throws Exception {
        request.addHeader("Authorization", "Bearer " + token(Map.of("tenantId", "tenant-1")));

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("swallows malformed tokens and continues the chain unauthenticated")
    void toleratesMalformedToken() throws Exception {
        request.addHeader("Authorization", "Bearer not.a.jwt");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
