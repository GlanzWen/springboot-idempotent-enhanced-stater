package com.glanz.idempotent.core;

/**
 * @author zz
 * 幂等处理器接口
 */
public interface IdempotentHandler {
    boolean tryAcquire(String key, long expireSeconds) throws Exception;
    default void release(String key) {}
}
