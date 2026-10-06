package com.lifeshare.service.impl;

import com.lifeshare.dto.CommentCreateDTO;
import com.lifeshare.entity.Comment;
import com.lifeshare.entity.Post;
import com.lifeshare.entity.User;
import com.lifeshare.repository.CommentRepository;
import com.lifeshare.repository.PostRepository;
import com.lifeshare.service.CommentCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentCommandServiceImpl implements CommentCommandService {
    
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    
    public CommentCommandServiceImpl(CommentRepository commentRepository, 
                                      PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }
    
    @Override
    @Transactional
    public Comment create(Long userId, CommentCreateDTO dto) {
        Comment comment = new Comment();
        comment.setContent(dto.getContent());
        
        User user = new User();
        user.setId(userId);
        comment.setUser(user);
        
        Post post = new Post();
        post.setId(dto.getPostId());
        comment.setPost(post);
        
        if (dto.getParentId() != null) {
            Comment parent = commentRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            comment.setParent(parent);
        }
        
        Comment savedComment = commentRepository.save(comment);
        
        postRepository.findById(dto.getPostId()).ifPresent(p -> {
            p.setCommentCount(p.getCommentCount() + 1);
            postRepository.save(p);
        });
        
        return savedComment;
    }
    
    @Override
    @Transactional
    public void delete(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        
        postRepository.findById(comment.getPost().getId()).ifPresent(p -> {
            p.setCommentCount(Math.max(0, p.getCommentCount() - 1));
            postRepository.save(p);
        });
        
        commentRepository.deleteById(id);
    }
}
