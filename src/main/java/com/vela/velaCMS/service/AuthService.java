package com.vela.velaCMS.service;

import com.vela.velaCMS.dto.response.AuthResponse;
import com.vela.velaCMS.dto.request.LoginRequest;
import com.vela.velaCMS.dto.request.RegisterRequest;
import com.vela.velaCMS.dto.response.OTPVerificationResponse;
import com.vela.velaCMS.entity.OTPResendEvent;
import com.vela.velaCMS.entity.User;
import com.vela.velaCMS.entity.UserRegisteredEvent;
import com.vela.velaCMS.repository.OTPRepository;
import com.vela.velaCMS.repository.UserRepository;
import com.vela.velaCMS.security.AccessTokenUtil;
import com.vela.velaCMS.core.result.FailureType;
import com.vela.velaCMS.core.result.Result;
import com.vela.velaCMS.security.AuthenticatedUser;
import com.vela.velaCMS.security.OTPGenerator;
import com.vela.velaCMS.websocket.service.WebSocketSessionService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final OTPRepository otpRepository;
    private final OTPGenerator otpGenerator;
    private final AccessTokenUtil accessTokenUtil;
    private final StimulusService stimulusService;
    private final WebSocketSessionService webSocketSessionService;
    private final BCryptPasswordEncoder encoder;
    private final ApplicationEventPublisher eventPublisher;

    public static final String COOKIE_NAME = "stimulus";

    @Autowired
    public AuthService(
            UserRepository userRepository,
            UserService userService,
            OTPRepository otpRepository,
            OTPGenerator otpGenerator,
            AccessTokenUtil accessTokenUtil,
            StimulusService stimulusService,
            WebSocketSessionService sessionService,
            BCryptPasswordEncoder encoder,
            ApplicationEventPublisher eventPublisher
    ){
        this.userRepository=userRepository;
        this.userService = userService;
        this.otpRepository = otpRepository;
        this.otpGenerator = otpGenerator;
        this.accessTokenUtil = accessTokenUtil;
        this.stimulusService=stimulusService;
        this.webSocketSessionService = sessionService;
        this.encoder = encoder;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<AuthResponse> register(@NotNull RegisterRequest request, HttpServletResponse response){
        if(userRepository.findByEmail(request.email().toLowerCase())!=null)
            return Result.failure("User already exists", FailureType.USER_ALREADY_EXISTS);

        User user = User.builder()
                .id(new ObjectId())
                .username(request.username())
                .email(request.email().toLowerCase())
                .password(encoder.encode(request.password()))
                .roles(List.of("USER"))
                .isVerified(false)
                .build();

        userRepository.insert(user);

        String rawOTP = generateOTP(user.getId().toString());

        eventPublisher.publishEvent(
                new UserRegisteredEvent(user.getEmail(), rawOTP)
        );

        return generateNewCookie(user.getId().toString())
                .map(cookie -> {
                    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
                    return new AuthResponse(
                            accessTokenUtil.generateAccessToken(user.getUsername()),
                            user.isVerified()
                    );
                });
    }

    public Result<AuthResponse> login(LoginRequest request, HttpServletResponse response) {
        User dbUser = userRepository.findByEmail(request.email().toLowerCase());
        if (dbUser == null)
            return Result.failure(FailureType.USER_NOT_FOUND);

        if(!encoder.matches(request.password(), dbUser.getPassword()))
            return Result.failure(FailureType.INVALID_CREDENTIALS);

        this.invalidateUserSession(dbUser.getUsername(), dbUser.getId().toString());

        return generateNewCookie(dbUser.getId().toString())
                .map(cookie -> {
                    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
                    return new AuthResponse(
                            accessTokenUtil.generateAccessToken(dbUser.getUsername()),
                            dbUser.isVerified()
                    );
                });
    }

    public Result<AuthResponse> refresh(String stimulus) {
        return stimulusService.validate(stimulus)
                .map(ctx ->
                        new AuthResponse(
                        accessTokenUtil.generateAccessToken(ctx.username()),
                                ctx.verified()
                        ));
    }

    private String generateOTP(String userID) {
        String otp = otpGenerator.generateOTP();
        String hashedOTP = encoder.encode(otp);
        otpRepository.saveHashedOTP(userID, hashedOTP);
        return otp;
    }

    public Result<?> resendOTP(AuthenticatedUser user, String ip) {
        if (user.isVerified())
            return Result.success(null);

        long check = otpRepository.reserveResend(user.getId(), ip);
        if(check != 0) return Result.failure(FailureType.TOO_MANY_REQUESTS);

        String rawOTP = generateOTP(user.getId());
        eventPublisher.publishEvent(new OTPResendEvent(user.getEmail(), rawOTP));
        return Result.success(null);
    }

    public Result<OTPVerificationResponse> verify(AuthenticatedUser user, String otp) {
        if (user.isVerified())
            return Result.success(new OTPVerificationResponse(true));

        long check = otpRepository.reserveAttempt(user.getId());
        if(check == -1)
            return Result.failure(FailureType.OTP_EXPIRED);
        else if(check == 0)
            return Result.failure(FailureType.TOO_MANY_REQUESTS);

        String hashedOTP = otpRepository.getHashedOTP(user.getId());
        if(hashedOTP == null)
            return Result.failure(FailureType.OTP_EXPIRED);

        boolean matches = encoder.matches(otp, hashedOTP);
        if(matches) {
            userService.markVerified(user.getId(), user.getUsername());
            return Result.success(new OTPVerificationResponse(true));
        }

        return Result.failure(FailureType.INVALID_CREDENTIALS);
    }

    public Result<?> logout(AuthenticatedUser user) {
        this.invalidateUserSession(user.getUsername(), user.getId());
        return Result.success(null);
    }

    public void invalidateUserSession(String username, String userID) {
        accessTokenUtil.deleteJWT(username);
        stimulusService.invalidate(userID);
        webSocketSessionService.logoutUser(username);
    }

    public String generateUserToken(AuthenticatedUser user) {
        String token = encoder.encode(user.getUsername() + user.getEmail());
        userService.saveToken(user.getId(), user.getUsername(), token);
        return token;
    }

    private Result<ResponseCookie> generateNewCookie(String userId){
        return stimulusService
                    .generateStimulus(userId, null)
                    .map(stimulus ->
                            ResponseCookie.from(COOKIE_NAME, stimulus)
                                    .httpOnly(true)
                                    .sameSite("None") //previously Strict
                                    .secure(true) // production - true
                                    .path("/cms/auth/refresh")
                                    .maxAge(Duration.ofDays(14))
                                    .build());
    }
}