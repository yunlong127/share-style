package com.lifeshare.service.impl;

import com.lifeshare.entity.User;
import com.lifeshare.repository.UserRepository;
import com.lifeshare.service.UserQueryService;
import org.springframework.stereotype.Service;

@Service
public class UserQueryServiceImpl implements UserQueryService {
    
    private final UserRepository userRepository;
    
    public UserQueryServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
    
    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}
