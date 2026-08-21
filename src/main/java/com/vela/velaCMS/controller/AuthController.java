package com.vela.velaCMS.controller;

import com.vela.velaCMS.dto.response.AuthResponse;
import com.vela.velaCMS.dto.request.LoginRequest;
import com.vela.velaCMS.dto.request.RegisterRequest;
import com.vela.velaCMS.dto.response.OTPVerificationResponse;
import com.vela.velaCMS.security.AuthenticatedUser;
import com.vela.velaCMS.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static com.vela.velaCMS.service.AuthService.COOKIE_NAME;

@RestController
@RequestMapping("/cms/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService){
        this.authService=authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response){
        AuthResponse response1 = authService.register(request, response).getOrThrow();
        return ResponseEntity.status(HttpStatus.CREATED).body(response1);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response){
        AuthResponse response1 = authService.login(request, response).getOrThrow();
        return ResponseEntity.status(HttpStatus.OK).body(response1);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(value = COOKIE_NAME) String stimulus){
        if(stimulus == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing refresh token cookie");
        AuthResponse response = authService.refresh(stimulus).getOrThrow();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify/{code}")
    public ResponseEntity<?> verify(@PathVariable String code, @AuthenticationPrincipal AuthenticatedUser user) {
        if(user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new OTPVerificationResponse(false));
        OTPVerificationResponse result = authService.verify(user, code).getOrThrow();
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/resend")
    public ResponseEntity<?> resendOTP(@AuthenticationPrincipal AuthenticatedUser user, HttpServletRequest request) {
        if(user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        authService.resendOTP(user, request.getRemoteAddr()).getOrThrow();
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal AuthenticatedUser user) {
        if(user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        authService.logout(user).getOrThrow();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@AuthenticationPrincipal AuthenticatedUser user) {
        if(user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        return ResponseEntity.ok(authService.generateUserToken(user));
    }

    @GetMapping("/check")
    public void check(){
    }
}
