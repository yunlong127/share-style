package com.lifeshare.service.impl;

import com.lifeshare.dto.UserRegisterDTO;
import com.lifeshare.entity.User;
import com.lifeshare.repository.UserRepository;
import com.lifeshare.service.UserCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCommandServiceImpl implements UserCommandService {
    
    private final UserRepository userRepository;
    
    public UserCommandServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    @Transactional
    public User register(UserRegisterDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        return userRepository.save(user);
    }
    
    @Override
    @Transactional
    public User updateProfile(Long id, String nickname, String bio, String avatar) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (nickname != null) user.setNickname(nickname);
        if (bio != null) user.setBio(bio);
        if (avatar != null) user.setAvatar(avatar);
        return userRepository.save(user);
    }
}
