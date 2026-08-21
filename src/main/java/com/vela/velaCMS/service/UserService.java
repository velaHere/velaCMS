package com.vela.velaCMS.service;

import com.vela.velaCMS.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository repository;

    @Autowired
    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    @CacheEvict(
            cacheNames = "authenticatedUsers",
            key = "#ignoredUsername"
    )
    public void saveToken(String userID, String ignoredUsername, String token) {
        repository.update(userID, "token", token);
    }

    @CacheEvict(
            cacheNames = "authenticatedUsers",
            key = "#ignoredUsername"
    )
    public void markVerified(String userID, String ignoredUsername) {
        repository.update(userID, "isVerified", true);
    }
}