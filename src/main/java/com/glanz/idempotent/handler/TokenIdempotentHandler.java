package com.glanz.idempotent.handler;

import com.glanz.idempotent.core.IdempotentHandler;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zz
 * 基于内存的 Token 实现（单机 demo 用）。
 * 不建议生产环境使用
 */
@Deprecated()
@Component("tokenIdempotentHandler")
public class TokenIdempotentHandler implements IdempotentHandler {

    private final Set<String> used = ConcurrentHashMap.newKeySet();

    @Override
    public boolean tryAcquire(String key, long expireSeconds) {
        return used.add(key);
    }

    @Override
    public void release(String key) {
        used.remove(key);
    }
}
