package com.lifeshare.service.impl;

import com.lifeshare.entity.Comment;
import com.lifeshare.repository.CommentRepository;
import com.lifeshare.service.CommentQueryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentQueryServiceImpl implements CommentQueryService {
    
    private final CommentRepository commentRepository;
    
    public CommentQueryServiceImpl(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }
    
    @Override
    public List<Comment> findByPostId(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtDesc(postId);
    }
    
    @Override
    public Comment findById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
    }
}
