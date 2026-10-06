package com.lifeshare.service;

import com.lifeshare.entity.User;

public interface UserQueryService {
    User findById(Long id);
    User findByUsername(String username);
    boolean existsByUsername(String username);
}
