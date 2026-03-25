package com.mahmoud.reservation.security.jwt;

import com.mahmoud.reservation.security.user.ShopUserDetailsService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final ShopUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println("=== FILTER START ===");

        String jwt = parseJwt(request);
        System.out.println("JWT: " + jwt);

        try {
            if (jwt != null) {

                boolean valid = jwtUtils.validate(jwt);
                System.out.println("VALID: " + valid);

                if (valid) {
                    String email = jwtUtils.extractUsername(jwt);
                    System.out.println("EMAIL: " + email);

                    var userDetails = userDetailsService.loadUserByUsername(email);

                    var authentication = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    System.out.println("AUTH SET SUCCESS");
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            SecurityContextHolder.clearContext();
        }

        System.out.println("=== FILTER END ===");

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }

        return header.substring(7);
    }
}