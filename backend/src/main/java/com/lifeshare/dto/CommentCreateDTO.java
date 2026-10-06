package com.lifeshare.dto;

import lombok.Data;

@Data
public class CommentCreateDTO {
    private Long postId;
    private String content;
    private Long parentId;
}
