package com.lifeshare.dto;

import lombok.Data;

@Data
public class UserResponseDTO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String bio;
}
