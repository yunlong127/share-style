package com.lifeshare.strategy;

import com.lifeshare.entity.Post;

public interface ContentProcessStrategy {
    String getType();
    void process(Post post);
    String getDisplayContent(Post post);
}
