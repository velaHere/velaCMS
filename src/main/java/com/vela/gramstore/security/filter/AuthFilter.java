package com.vela.gramstore.security.filter;

import com.vela.gramstore.security.AccessTokenUtil;
import jakarta.annotation.Nonnull;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class AuthFilter extends OncePerRequestFilter {

    private final AccessTokenUtil accessTokenUtil;
    private final UserDetailsService userDetailsService;

    public AuthFilter(AccessTokenUtil accessTokenUtil, UserDetailsService userDetailsService){
        this.accessTokenUtil = accessTokenUtil;
        this.userDetailsService=userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, @Nonnull HttpServletResponse response, @Nonnull FilterChain filterChain) throws ServletException, IOException {

        String auth = req.getHeader("Authorization");
        try{
            if (SecurityContextHolder.getContext().getAuthentication() != null) return;

            if (auth == null || !auth.startsWith("Bearer ")) return;
            String token = auth.substring("Bearer ".length());
            if (accessTokenUtil.isTokenExpiredOrInvalid(token)) return;

            String username = accessTokenUtil.verifyAndExtractUsername(token);
            if (username == null) return;

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }catch(Exception e){
            log.error("Error(Custom): ", e);
        }finally{
            filterChain.doFilter(req, response);
        }
    }
}
