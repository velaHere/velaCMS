package com.vela.velaCMS.repository;

import com.vela.velaCMS.config.property.AppProperties;
import com.vela.velaCMS.core.result.FailureType;
import com.vela.velaCMS.core.result.Result;
import com.vela.velaCMS.core.domain.StimulusContext;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class StimulusRepository {

    public final String SIGN_PREFIX_FIELD;
    private final String KEY_PREFIX;
    private final int ttlDays;
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public StimulusRepository(AppProperties props, RedisTemplate<String, Object> redisTemplate){
        this.redisTemplate=redisTemplate;
        this.SIGN_PREFIX_FIELD=props.redis().sessionHashSignaturePrefixField();
        this.KEY_PREFIX=props.redis().sessionHashKeyPrefix();
        this.ttlDays=props.security().stimulus().ttlDays();
    }

    public Result<String> saveStimulusRecord(@Nullable String currentSessionId, String userId, String signaturePrefix){
        if(currentSessionId != null){
            return updateStimulusRecord(currentSessionId, signaturePrefix);
        }
        return Result.wrap(() -> {
            String redisKey = KEY_PREFIX+ userId;
            redisTemplate.opsForValue().set(redisKey, signaturePrefix, ttlDays, TimeUnit.DAYS);
            return userId;
        });
    }

    public void deleteStimulus(String userID) {
        redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + userID);
    }

    public Result<StimulusContext> fetchSession(String sessionId){

        String key = KEY_PREFIX + sessionId;

        if(!redisTemplate.hasKey(key))
            return Result.failure("Session not found", FailureType.SESSION_NOT_FOUND);

        Object signaturePrefix = redisTemplate.opsForValue().get(key);

        if(signaturePrefix == null)
            return Result.failure("Empty Session details", FailureType.INTERNAL);

        return Result.success(new StimulusContext(sessionId, null, false, signaturePrefix.toString()));
    }

    private Result<String> updateStimulusRecord(String sessionId , String signaturePrefix){

        String key = KEY_PREFIX + sessionId;
        if(!redisTemplate.hasKey(key)){
            return Result.failure("Session not found", FailureType.SESSION_NOT_FOUND);
        }

        return Result.wrap(() -> {
            redisTemplate
                    .opsForValue()
                    .set(KEY_PREFIX+sessionId, signaturePrefix, ttlDays, TimeUnit.DAYS);
                    return sessionId;
        });
    }
}
