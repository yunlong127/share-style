package com.lifeshare.controller;

import com.lifeshare.dto.CommentCreateDTO;
import com.lifeshare.entity.Comment;
import com.lifeshare.service.CommentCommandService;
import com.lifeshare.service.CommentQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    
    private final CommentQueryService commentQueryService;
    private final CommentCommandService commentCommandService;
    
    public CommentController(CommentQueryService commentQueryService, 
                             CommentCommandService commentCommandService) {
        this.commentQueryService = commentQueryService;
        this.commentCommandService = commentCommandService;
    }
    
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Comment>> getPostComments(@PathVariable Long postId) {
        return ResponseEntity.ok(commentQueryService.findByPostId(postId));
    }
    
    @PostMapping
    public ResponseEntity<Comment> createComment(
            @RequestParam Long userId,
            @RequestBody CommentCreateDTO dto) {
        return ResponseEntity.ok(commentCommandService.create(userId, dto));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        commentCommandService.delete(id);
        return ResponseEntity.ok().build();
    }
}
