package com.lifeshare.strategy.impl;

import com.lifeshare.entity.Post;
import com.lifeshare.strategy.ContentProcessStrategy;
import org.springframework.stereotype.Component;

@Component
public class ArticleContentStrategy implements ContentProcessStrategy {
    
    @Override
    public String getType() {
        return "article";
    }
    
    @Override
    public void process(Post post) {
        if (post.getContent() != null) {
            // 转义HTML标签，防止XSS攻击，同时保留代码完整性
            post.setContent(post.getContent().replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;"));
        }
    }
    
    @Override
    public String getDisplayContent(Post post) {
        if (post.getContent() == null) return "";
        // 直接使用转义后的内容，不需要再删除标签
        String summary = post.getContent();
        return summary.length() > 200 ? summary.substring(0, 200) + "..." : summary;
    }
}
