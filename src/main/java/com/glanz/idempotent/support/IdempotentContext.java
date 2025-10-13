package com.glanz.idempotent.support;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * @author zz
 * 内存上下文：保存 key -> value（value 用于在 release 时比对）
 * 仅在同一应用实例内有效，分布式场景释放仍以 Redis 脚本为准。
 */
public class IdempotentContext {

    private static final ConcurrentHashMap<String, String> KEY_VAL = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ScheduledFuture<?>> RENEW_TASKS = new ConcurrentHashMap<>();

    public static void putVal(String key, String val) { KEY_VAL.put(key, val); }
    public static String getVal(String key) { return KEY_VAL.get(key); }
    public static void removeVal(String key) { KEY_VAL.remove(key); }

    public static void putRenewTask(String key, ScheduledFuture<?> task) { RENEW_TASKS.put(key, task); }
    public static ScheduledFuture<?> getRenewTask(String key) { return RENEW_TASKS.get(key); }
    public static void removeRenewTask(String key) {
        ScheduledFuture<?> f = RENEW_TASKS.remove(key);
        if (f != null) {
            f.cancel(true);
        }
    }

    public static void cleanup(String key) {
        removeVal(key);
        removeRenewTask(key);
    }
}
