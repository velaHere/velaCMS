package com.vela.gramstore.service;

import com.vela.gramstore.config.property.AppProperties;
import com.vela.gramstore.entity.User;
import com.vela.gramstore.repository.StimulusRepository;
import com.vela.gramstore.repository.UserRepository;
import com.vela.gramstore.core.result.FailureType;
import com.vela.gramstore.core.result.Result;
import com.vela.gramstore.core.domain.StimulusContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
public class StimulusService {

    private final String ALGORITHM;
    private final String APPENDER;
    private final UserRepository userRepository;
    private final SecretKeySpec secretKeySpec;
    private final StimulusRepository stimulusRepository;
    private static final SecureRandom sRandom=new SecureRandom();

    @Autowired
    public StimulusService(
            AppProperties props,
            UserRepository userRepository,
            StimulusRepository stimulusRepository
    ){
        this.ALGORITHM=props.security().stimulus().algorithm();
        this.APPENDER=props.security().stimulus().appender();
        this.userRepository=userRepository;
        this.stimulusRepository = stimulusRepository;
        this.secretKeySpec=new SecretKeySpec(
                props.security().stimulus().secret().getBytes(StandardCharsets.UTF_8),
                ALGORITHM
        );
    }

    public Result<String> generateStimulus(String userId, String oldStimulus){
        return resolveSessionId(oldStimulus)
                .flatMap(sessionId -> generateAndSave(sessionId, userId))
                .flatMap(ctx ->
                        generateSignature(ctx.signaturePrefix())
                                .map(signature -> buildToken(ctx.sessionId(), signature))
                );
    }

    private Result<SaveContext> generateAndSave(@Nullable String sessionId, String userId){
        String signaturePrefix = randomBase64();
        return stimulusRepository
                .saveStimulusRecord(sessionId, userId, signaturePrefix)
                .map(savedSessionId -> new SaveContext(savedSessionId, signaturePrefix));
    }

    public Result<StimulusContext> validate(String stimulus){
        String token = extractTokenFromStimulus(stimulus);
        return parseToken(token)
                .flatMap(parts -> fetchAndVerify(parts[0], parts[1]));
    }

    public void invalidate(String userID) {
        stimulusRepository.deleteStimulus(userID);
    }

    private String buildToken(String sessionId, String signature){
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (sessionId + APPENDER + signature).getBytes(StandardCharsets.UTF_8)
        );
    }

    private Result<String> generateSignature(String data){ // data -> signaturePrefix
        return Result.wrap(() -> {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(secretKeySpec);
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(mac.doFinal(
                            data.getBytes(StandardCharsets.UTF_8)));
        });
    }

    private boolean constantTimeEquals(String a, String b){
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    private String randomBase64(){
        byte [] bytes = new byte[6];
        sRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Result<String> resolveSessionId(@Nullable String oldStimulus){
        if(oldStimulus==null) return Result.success(null);
        return extractSessionId(oldStimulus);
    }

    private Result<String[]> parseToken(String rawToken){

        if (rawToken == null || !rawToken.contains(APPENDER))
            return Result.failure("Malformed token", FailureType.MALFORMED_TOKEN);

        String [] parts = rawToken.split(APPENDER);
        if(parts.length != 2)
            return Result.failure("Malformed token", FailureType.MALFORMED_TOKEN);

        return Result.success(parts);
    }

    private Result<StimulusContext> fetchAndVerify(String sessionId, String incomingSignature){
        return stimulusRepository.fetchSession(sessionId)
                .flatMap(this::verifyUser)
                .flatMap(ctx -> verifySignature(ctx, incomingSignature));
    }

    private Result<StimulusContext> verifySignature(StimulusContext ctx, String incomingSignature){
        return generateSignature(ctx.signaturePrefix())
                .flatMap(expectedSignature -> {
                    if(!constantTimeEquals(expectedSignature, incomingSignature))
                        return Result.failure("Invalid Signature", FailureType.TAMPERED_TOKEN);
                    return Result.success(ctx);
                });
    }

    private Result<StimulusContext> verifyUser(StimulusContext ctx){

        Optional<User> dbUser = userRepository.findById(new ObjectId(ctx.userId()));

        return dbUser.map(user ->
                Result.success(new StimulusContext(ctx.userId(), user.getUsername(), user.isVerified(), ctx.signaturePrefix())))
                .orElseGet(() -> Result.failure("User not found", FailureType.USER_NOT_FOUND));
    }

    private String extractTokenFromStimulus(String stimulus){
        return new String(Base64.getUrlDecoder().decode(stimulus), StandardCharsets.UTF_8);
    }

    public Result<String> extractSessionId(@Nullable String stimulus){

        if(stimulus==null)
            return Result.failure("Malformed Stimulus", FailureType.MALFORMED_TOKEN);

        String token = extractTokenFromStimulus(stimulus);

        if(!token.contains(APPENDER))
            return Result.failure("Malformed Stimulus", FailureType.MALFORMED_TOKEN);

        return Result.wrap(() -> token.split(APPENDER)[0]);
    }

    private record SaveContext(String sessionId, String signaturePrefix){}
}
