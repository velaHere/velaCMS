package com.vela.velaCMS.service;

import com.vela.velaCMS.entity.User;
import com.vela.velaCMS.repository.UserRepository;
import com.vela.velaCMS.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public UserDetailsServiceImpl(UserRepository userRepository){
        this.userRepository=userRepository;
    }

    @Override
    @Cacheable(
            cacheNames = "authenticatedUsers",
            key = "#username",
            sync = true
    )
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if(user!=null){
            return new AuthenticatedUser(
                    user.getId().toString(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getToken(),
                    user.isVerified(),
                    user.getRoles()
            );
        }
        throw new UsernameNotFoundException("User not found with username: " + username);
    }
}
