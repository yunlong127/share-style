package com.lifeshare.controller;

import com.lifeshare.dto.UserLoginDTO;
import com.lifeshare.dto.UserRegisterDTO;
import com.lifeshare.dto.UserResponseDTO;
import com.lifeshare.entity.User;
import com.lifeshare.service.UserCommandService;
import com.lifeshare.service.UserQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;
    
    public UserController(UserQueryService userQueryService, UserCommandService userCommandService) {
        this.userQueryService = userQueryService;
        this.userCommandService = userCommandService;
    }
    
    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@RequestBody UserRegisterDTO dto) {
        User user = userCommandService.register(dto);
        return ResponseEntity.ok(toUserResponseDTO(user));
    }
    
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody UserLoginDTO dto) {
        Map<String, Object> response = new HashMap<>();
        
        User user = userQueryService.findByUsername(dto.getUsername());
        if (user == null) {
            response.put("status", "not_registered");
            response.put("message", "用户未注册");
            return ResponseEntity.ok(response);
        } else if (!user.getPassword().equals(dto.getPassword())) {
            response.put("status", "password_error");
            response.put("message", "密码错误");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "success");
            response.put("message", "登录成功");
            response.put("user", toUserResponseDTO(user));
            return ResponseEntity.ok(response);
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUser(@PathVariable Long id) {
        User user = userQueryService.findById(id);
        return ResponseEntity.ok(toUserResponseDTO(user));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateProfile(
            @PathVariable Long id,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) String avatar) {
        User user = userCommandService.updateProfile(id, nickname, bio, avatar);
        return ResponseEntity.ok(toUserResponseDTO(user));
    }
    
    private UserResponseDTO toUserResponseDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setBio(user.getBio());
        return dto;
    }
}
