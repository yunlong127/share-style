package com.lifeshare.service;

import com.lifeshare.dto.CommentCreateDTO;
import com.lifeshare.entity.Comment;

public interface CommentCommandService {
    Comment create(Long userId, CommentCreateDTO dto);
    void delete(Long id);
}
