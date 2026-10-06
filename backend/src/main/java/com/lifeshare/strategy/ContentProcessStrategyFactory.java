package com.lifeshare.strategy;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ContentProcessStrategyFactory {
    
    private final Map<String, ContentProcessStrategy> strategyMap = new HashMap<>();
    
    public ContentProcessStrategyFactory(List<ContentProcessStrategy> strategies) {
        for (ContentProcessStrategy strategy : strategies) {
            strategyMap.put(strategy.getType(), strategy);
        }
    }
    
    public ContentProcessStrategy getStrategy(String contentType) {
        ContentProcessStrategy strategy = strategyMap.get(contentType);
        if (strategy == null) {
            strategy = strategyMap.get("article");
        }
        return strategy;
    }
    
    public void registerStrategy(ContentProcessStrategy strategy) {
        strategyMap.put(strategy.getType(), strategy);
    }
}
