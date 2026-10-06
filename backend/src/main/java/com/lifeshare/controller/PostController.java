package com.lifeshare.controller;

import com.lifeshare.dto.PostCreateDTO;
import com.lifeshare.dto.PostUpdateDTO;
import com.lifeshare.entity.Post;
import com.lifeshare.service.PostCommandService;
import com.lifeshare.service.PostQueryService;
import com.lifeshare.strategy.ContentProcessStrategy;
import com.lifeshare.strategy.ContentProcessStrategyFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    
    private final PostQueryService postQueryService;
    private final PostCommandService postCommandService;
    private final ContentProcessStrategyFactory strategyFactory;
    
    public PostController(PostQueryService postQueryService,
                          PostCommandService postCommandService,
                          ContentProcessStrategyFactory strategyFactory) {
        this.postQueryService = postQueryService;
        this.postCommandService = postCommandService;
        this.strategyFactory = strategyFactory;
    }
    
    @GetMapping
    public ResponseEntity<Page<Post>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(postQueryService.findAll(pageRequest));
    }
    
    @GetMapping("/type/{contentType}")
    public ResponseEntity<Page<Post>> getPostsByType(
            @PathVariable String contentType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(postQueryService.findByContentType(contentType, pageRequest));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<Post>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(postQueryService.findByUserId(userId, pageRequest));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPost(@PathVariable Long id) {
        postCommandService.incrementViewCount(id);
        Post post = postQueryService.findById(id);
        
        ContentProcessStrategy strategy = strategyFactory.getStrategy(post.getContentType());
        
        Map<String, Object> response = new HashMap<>();
        response.put("post", post);
        response.put("displayContent", strategy.getDisplayContent(post));
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    public ResponseEntity<Post> createPost(
            @RequestParam Long userId,
            @RequestBody PostCreateDTO dto) {
        return ResponseEntity.ok(postCommandService.create(userId, dto));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Post> updatePost(
            @PathVariable Long id,
            @RequestBody PostUpdateDTO dto) {
        return ResponseEntity.ok(postCommandService.update(id, dto));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postCommandService.delete(id);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likePost(@PathVariable Long id) {
        postCommandService.incrementLikeCount(id);
        return ResponseEntity.ok().build();
    }
}
