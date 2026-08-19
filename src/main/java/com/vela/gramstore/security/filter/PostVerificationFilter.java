package com.vela.gramstore.security.filter;

import com.vela.gramstore.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class PostVerificationFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;

    @Autowired
    public PostVerificationFilter(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !path.matches("^/[^/]+/post$") && !path.matches("^/[^/]+/post/[^/]+$");
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {

        try {
            if (SecurityContextHolder.getContext().getAuthentication() != null) return;

            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) return;
            String token = auth.substring("Bearer ".length());

            String[] split = request.getServletPath().split("/");
            if(split.length < 2) return;
            String username = split[1];

            AuthenticatedUser user = (AuthenticatedUser) userDetailsService.loadUserByUsername(username);
            if (user.getToken().equals(token))
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));

        } catch (Exception e) {
            logger.error("Error(Custom): ", e);
        } finally {
            filterChain.doFilter(request, response);
        }
    }
}