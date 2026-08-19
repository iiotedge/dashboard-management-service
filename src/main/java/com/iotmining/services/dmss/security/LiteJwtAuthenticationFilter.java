package com.iotmining.services.dmss.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LiteJwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secretKeyBase64;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKeyBase64));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        try {
            // 1. Decode token without hitting the database
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 2. Extract roles directly from the token payload. Read the raw claim
            // and check its runtime type rather than claims.get("role", List.class) -
            // JJWT's typed getter throws instead of converting for some JSON array
            // shapes (observed with an empty array), which would silently drop this
            // entire authentication attempt in the catch block below rather than
            // just fail to find any roles.
            Object rolesClaim = claims.get("role");
            List<SimpleGrantedAuthority> authorities = rolesClaim instanceof java.util.Collection<?> rawRoles
                    ? rawRoles.stream().map(String::valueOf).map(SimpleGrantedAuthority::new).collect(Collectors.toList())
                    : List.of();

            // 3. Create a lightweight Authentication object.
            // We pass the entire 'claims' object as the Principal so our Evaluator can read it easily!
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    claims,  // Principal is now the Claims map
                    null,    // No credentials needed
                    authorities
            );

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

        } catch (Exception e) {
            log.warn("Lite JWT Validation failed in DMSS: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}