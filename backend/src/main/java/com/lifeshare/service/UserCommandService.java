package com.lifeshare.service;

import com.lifeshare.dto.UserRegisterDTO;
import com.lifeshare.entity.User;

public interface UserCommandService {
    User register(UserRegisterDTO dto);
    User updateProfile(Long id, String nickname, String bio, String avatar);
}
