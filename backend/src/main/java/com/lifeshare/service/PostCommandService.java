package com.lifeshare.service;

import com.lifeshare.dto.PostCreateDTO;
import com.lifeshare.dto.PostUpdateDTO;
import com.lifeshare.entity.Post;

public interface PostCommandService {
    Post create(Long userId, PostCreateDTO dto);
    Post update(Long id, PostUpdateDTO dto);
    void delete(Long id);
    void incrementViewCount(Long id);
    void incrementLikeCount(Long id);
}
