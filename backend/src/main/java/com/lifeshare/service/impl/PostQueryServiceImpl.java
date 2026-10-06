package com.lifeshare.service.impl;

import com.lifeshare.entity.Post;
import com.lifeshare.repository.PostRepository;
import com.lifeshare.service.PostQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PostQueryServiceImpl implements PostQueryService {
    
    private final PostRepository postRepository;
    
    public PostQueryServiceImpl(PostRepository postRepository) {
        this.postRepository = postRepository;
    }
    
    @Override
    public Post findById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
    }
    
    @Override
    public Page<Post> findAll(Pageable pageable) {
        return postRepository.findAll(pageable);
    }
    
    @Override
    public Page<Post> findByContentType(String contentType, Pageable pageable) {
        return postRepository.findByContentType(contentType, pageable);
    }
    
    @Override
    public Page<Post> findByUserId(Long userId, Pageable pageable) {
        return postRepository.findByUserId(userId, pageable);
    }
}
