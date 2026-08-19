package com.vela.gramstore.config;

import com.vela.gramstore.security.*;
import com.vela.gramstore.security.filter.AuthFilter;
import com.vela.gramstore.security.filter.EmailVerificationFilter;
import com.vela.gramstore.security.filter.PostVerificationFilter;
import com.vela.gramstore.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SpringSecurity {

    private final UserDetailsServiceImpl userDetailsService;
    private final JWTAuthenticationEntryPoint authenticationEntryPoint;
    private final EmailVerificationFilter emailVerificationFilter;
    private final PostVerificationFilter postVerificationFilter;

    @Autowired
    public SpringSecurity(
            UserDetailsServiceImpl userDetailsService,
            JWTAuthenticationEntryPoint authenticationEntryPoint,
            EmailVerificationFilter emailVerificationFilter,
            PostVerificationFilter postVerificationFilter
    ){
        this.userDetailsService=userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.emailVerificationFilter = emailVerificationFilter;
        this.postVerificationFilter = postVerificationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthFilter authFilter) throws Exception{
        return http.authorizeHttpRequests(request -> request
                        .requestMatchers(
                                "/cms/auth/login",
                                "/cms/auth/register",
                                "/cms/auth/refresh",
                                "/image/**",
                                "/ws/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(postVerificationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(authFilter, PostVerificationFilter.class)
                .addFilterAfter(emailVerificationFilter, AuthFilter.class)
                .exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint))
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session->session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .userDetailsService(userDetailsService)
                .headers(headers -> headers
                        .contentSecurityPolicy(csp ->
                                csp.policyDirectives("connect-src 'self' ws://localhost:8080")
                        )
                )
                .build();
    }

    @Bean
    public AuthFilter getJwtAuthFilter(AccessTokenUtil accessTokenUtil){
        return new AuthFilter(accessTokenUtil, userDetailsService);
    }

    @Bean
    public BCryptPasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }
}
