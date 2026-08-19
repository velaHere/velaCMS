package com.vela.gramstore.security;

import com.vela.gramstore.config.property.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import static java.sql.Date.from;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class AccessTokenUtil {

    private final SecretKey secretKey;
    private final String issuer;
    private final long accessTokenMinutes;
    private final RedisTemplate<String, Object> redisTemplate;
    private final String ACCESS_TOKEN_PREFIX;

    @Autowired
    public AccessTokenUtil(AppProperties props,
                           RedisTemplate<String, Object> redisTemplate){
        byte[] keyBytes = Base64.getDecoder().decode(props.security().accessToken().secret());
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.issuer=props.security().accessToken().issuer();
        this.accessTokenMinutes=props.security().accessToken().ttlMins();
        this.ACCESS_TOKEN_PREFIX=props.redis().accessTokenKeyPrefix();
        this.redisTemplate=redisTemplate;
    }

    public String verifyAndExtractUsername(String token){
        return extractAllClaims(token).getSubject();
    }

    public Date extractExpiration(String token){
        return extractAllClaims(token).getExpiration();
    }

    private Claims extractAllClaims(String token){
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenExpiredOrInvalid(@Nullable String token){
        if(token == null) return true;
        String username = verifyAndExtractUsername(token);
        String stored = (String) redisTemplate.opsForValue().get(ACCESS_TOKEN_PREFIX + username);
        return extractExpiration(token).before(new Date()) || stored==null || !stored.equals(token);
    }

    public void deleteJWT(String username) {
        redisTemplate.opsForValue().getAndDelete(ACCESS_TOKEN_PREFIX + username);
    }

    public String generateAccessToken(String username){
        Map<String, Object> claims = new HashMap<>();
        String accessToken = createAccessToken(claims, username);
        redisTemplate.opsForValue().set(
                ACCESS_TOKEN_PREFIX+username,
                accessToken,
                accessTokenMinutes,
                TimeUnit.MINUTES);
        return accessToken;
    }

    private String createAccessToken(Map<String, Object> claims, String subject){
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessTokenMinutes*60);
        return Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .claims(claims)
                .header().empty().add("typ", "JWT")
                .and()
                .issuedAt(from(now))
                .expiration(from(exp))
                .signWith(secretKey)
                .compact();
    }
}
