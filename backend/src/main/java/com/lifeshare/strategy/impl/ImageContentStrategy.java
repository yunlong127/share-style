package com.lifeshare.strategy.impl;

import com.lifeshare.entity.Post;
import com.lifeshare.strategy.ContentProcessStrategy;
import org.springframework.stereotype.Component;

@Component
public class ImageContentStrategy implements ContentProcessStrategy {
    
    @Override
    public String getType() {
        return "image";
    }
    
    @Override
    public void process(Post post) {
        if (post.getCoverImage() == null && post.getContent() != null) {
            String[] images = post.getContent().split(",");
            if (images.length > 0) {
                post.setCoverImage(images[0].trim());
            }
        }
    }
    
    @Override
    public String getDisplayContent(Post post) {
        int imageCount = post.getContent() != null ? post.getContent().split(",").length : 0;
        return imageCount + " 张图片";
    }
}
