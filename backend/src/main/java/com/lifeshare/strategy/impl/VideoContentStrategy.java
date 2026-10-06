package com.lifeshare.strategy.impl;

import com.lifeshare.entity.Post;
import com.lifeshare.strategy.ContentProcessStrategy;
import org.springframework.stereotype.Component;

@Component
public class VideoContentStrategy implements ContentProcessStrategy {
    
    @Override
    public String getType() {
        return "video";
    }
    
    @Override
    public void process(Post post) {
        if (post.getContent() != null && post.getContent().contains("/uploads/") && post.getCoverImage() == null) {
            // 如果是服务器存储的视频，设置默认封面
            post.setCoverImage("/uploads/video-cover.jpg");
        }
    }
    
    @Override
    public String getDisplayContent(Post post) {
        return "视频分享";
    }
}
