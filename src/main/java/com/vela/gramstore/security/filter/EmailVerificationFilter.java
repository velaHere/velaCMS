package com.vela.gramstore.security.filter;

import com.vela.gramstore.entity.User;
import com.vela.gramstore.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class EmailVerificationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Autowired
    public EmailVerificationFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().matches("/cms/auth/verify/\\d{6}") ||
                request.getServletPath().equals("/cms/auth/resend") ||
                request.getServletPath().equals("/cms/auth/login") ||
                request.getServletPath().equals("/cms/auth/register") ||
                request.getServletPath().equals("/cms/auth/refresh");
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication instanceof UsernamePasswordAuthenticationToken) {
            Object principal = authentication.getPrincipal();

            if(principal instanceof UserDetails userDetails) {
                User user = userRepository.findByUsername(userDetails.getUsername());
                if(user == null) {
                    response.sendError(
                            HttpServletResponse.SC_FORBIDDEN,
                            "Email verification failed"
                    );
                    return;
                }

                if(!user.isVerified()) {
                    response.sendError(
                            HttpServletResponse.SC_FORBIDDEN,
                            "Email verification required"
                    );
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}