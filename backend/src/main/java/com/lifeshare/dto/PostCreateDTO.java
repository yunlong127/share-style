package com.lifeshare.dto;

import lombok.Data;

@Data
public class PostCreateDTO {
    private String title;
    private String content;
    private String contentType;
    private String coverImage;
}
