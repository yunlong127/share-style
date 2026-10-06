package com.lifeshare.service;

import com.lifeshare.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostQueryService {
    Post findById(Long id);
    Page<Post> findAll(Pageable pageable);
    Page<Post> findByContentType(String contentType, Pageable pageable);
    Page<Post> findByUserId(Long userId, Pageable pageable);
}
