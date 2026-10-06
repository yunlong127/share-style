package com.lifeshare.service.impl;

import com.lifeshare.dto.PostCreateDTO;
import com.lifeshare.dto.PostUpdateDTO;
import com.lifeshare.entity.Post;
import com.lifeshare.entity.User;
import com.lifeshare.repository.PostRepository;
import com.lifeshare.service.PostCommandService;
import com.lifeshare.strategy.ContentProcessStrategy;
import com.lifeshare.strategy.ContentProcessStrategyFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostCommandServiceImpl implements PostCommandService {
    
    private final PostRepository postRepository;
    private final ContentProcessStrategyFactory strategyFactory;
    
    public PostCommandServiceImpl(PostRepository postRepository, 
                                   ContentProcessStrategyFactory strategyFactory) {
        this.postRepository = postRepository;
        this.strategyFactory = strategyFactory;
    }
    
    @Override
    @Transactional
    public Post create(Long userId, PostCreateDTO dto) {
        Post post = new Post();
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setContentType(dto.getContentType() != null ? dto.getContentType() : "article");
        post.setCoverImage(dto.getCoverImage());
        
        User user = new User();
        user.setId(userId);
        post.setUser(user);
        
        ContentProcessStrategy strategy = strategyFactory.getStrategy(post.getContentType());
        strategy.process(post);
        
        return postRepository.save(post);
    }
    
    @Override
    @Transactional
    public Post update(Long id, PostUpdateDTO dto) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        if (dto.getTitle() != null) post.setTitle(dto.getTitle());
        if (dto.getContent() != null) post.setContent(dto.getContent());
        if (dto.getCoverImage() != null) post.setCoverImage(dto.getCoverImage());
        
        ContentProcessStrategy strategy = strategyFactory.getStrategy(post.getContentType());
        strategy.process(post);
        
        return postRepository.save(post);
    }
    
    @Override
    @Transactional
    public void delete(Long id) {
        postRepository.deleteById(id);
    }
    
    @Override
    @Transactional
    public void incrementViewCount(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setViewCount(post.getViewCount() + 1);
        postRepository.save(post);
    }
    
    @Override
    @Transactional
    public void incrementLikeCount(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setLikeCount(post.getLikeCount() + 1);
        postRepository.save(post);
    }
}
