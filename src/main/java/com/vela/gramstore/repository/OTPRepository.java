package com.vela.gramstore.repository;

import com.vela.gramstore.config.property.AppProperties;
import com.vela.gramstore.security.OTPKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class OTPRepository {

    private final String keyPrefix;
    private final String hashedOtpField;
    private final String attemptsField;

    private static final RedisScript<Long> RESERVE_ATTEMPT_SCRIPT =
            RedisScript.of(
                    new ClassPathResource(
                            "redis/otp/reserve_attempt.lua"
                    ),
                    Long.class
            );

    private static final RedisScript<Long> RESERVE_RESEND_RATE_LIMIT =
            RedisScript.of(
                    new ClassPathResource(
                            "redis/otp/resend_rate_limit.lua"
                    ),
                    Long.class
            );

    private final RedisTemplate<String, Object> template;

    @Autowired
    public OTPRepository(RedisTemplate<String, Object> template, AppProperties properties) {
        this.template = template;
        this.keyPrefix = properties.security().otp().cacheKeyPrefix();
        this.hashedOtpField = properties.security().otp().hashedOtpField();
        this.attemptsField = properties.security().otp().attemptsField();
    }

    public void saveHashedOTP(String userID, String hashedOTP) {
        String key = keyPrefix + userID;
        template.opsForHash().put(key, hashedOtpField, hashedOTP);
        template.opsForHash().put(key, attemptsField, 0);
        template.expire(key, Duration.ofMinutes(5));
    }

    public String getHashedOTP(String userID) {
        return (String) template.opsForHash().get(keyPrefix + userID, hashedOtpField);
    }

    public long reserveAttempt(String userID) {
        return template.execute(RESERVE_ATTEMPT_SCRIPT, List.of(keyPrefix +userID));
    }

    public long reserveResend(String userID, String ip) {
        return template.execute(
                RESERVE_RESEND_RATE_LIMIT,
                List.of(
                        OTPKeys.resendCooldown(userID),
                        OTPKeys.resendUserHourly(userID),
                        OTPKeys.resendIpHourly(ip)
                ),
                30, 5, 20, 3600
        );
    }
}
