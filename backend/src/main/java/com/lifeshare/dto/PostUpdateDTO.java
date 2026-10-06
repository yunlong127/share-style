package com.lifeshare.dto;

import lombok.Data;

@Data
public class PostUpdateDTO {
    private String title;
    private String content;
    private String coverImage;
}
