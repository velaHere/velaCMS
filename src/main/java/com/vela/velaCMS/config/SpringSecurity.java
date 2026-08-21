package com.vela.velaCMS.config;

import com.vela.velaCMS.security.*;
import com.vela.velaCMS.security.filter.AuthFilter;
import com.vela.velaCMS.security.filter.EmailVerificationFilter;
import com.vela.velaCMS.security.filter.PostVerificationFilter;
import com.vela.velaCMS.service.UserDetailsServiceImpl;
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
    private final EmailVerificationFilter emailVerificationFilter;
    private final PostVerificationFilter postVerificationFilter;

    @Autowired
    public SpringSecurity(
            UserDetailsServiceImpl userDetailsService,
            EmailVerificationFilter emailVerificationFilter,
            PostVerificationFilter postVerificationFilter
    ){
        this.userDetailsService=userDetailsService;
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
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(postVerificationFilter, AuthFilter.class)
                .addFilterAfter(emailVerificationFilter, PostVerificationFilter.class)
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
