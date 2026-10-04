package com.finflow.accountservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class InternalJwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecretKey secretKey;

    public InternalJwtAuthenticationFilter(@Value("${internal.jwt.secret}") String secretKey) {
        this.secretKey= Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException{

        String authHeader = request.getHeader("Authorization");

        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token=authHeader.substring(7);
        try{
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String subject = claims.getSubject();
            Boolean service=claims.get("service", Boolean.class);

            if("transaction-service".equals(subject) && Boolean.TRUE.equals(service)) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                subject,
                                null,
                                AuthorityUtils.createAuthorityList("ROLE_SERVICE")
                        );
                request.setAttribute("internal-service",subject);

                org.springframework.security.core.context.SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
            }
        }catch (Exception ignored){
            // Invalid internal token.
            // SecurityConfig will reject the protected request.
        }
        filterChain.doFilter(request, response);
    }
}
