package com.glanz.idempotent.core;

/**
 * @author zz
 * 幂等处理器接口
 */
public interface IdempotentHandler {

    /**
     * 加锁
     *
     * @param key 锁keu
     * @param expireSeconds 过期时间
     */
    boolean tryAcquire(String key, long expireSeconds) throws Exception;

    /**
     * 解锁
     * @param key 解锁key
     */
    default void release(String key) {}
}
