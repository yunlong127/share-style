package com.lifeshare.service;

import com.lifeshare.entity.Comment;
import java.util.List;

public interface CommentQueryService {
    List<Comment> findByPostId(Long postId);
    Comment findById(Long id);
}
