package com.glanz.idempotent.support;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 管理续期任务，优先复用外部 ScheduledExecutorService，否则内部创建。
 */
public class RedisLockRenewManager {

    private final DefaultRedisScript<Long> renewScript;
    private final AtomicBoolean ownedScheduler = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler;

    public RedisLockRenewManager(ScheduledExecutorService providedScheduler) {
        this.renewScript = new DefaultRedisScript<>();
        this.renewScript.setLocation(new ClassPathResource("scripts/idempotent_renew.lua"));
        this.renewScript.setResultType(Long.class);

        if (providedScheduler != null) {
            this.scheduler = providedScheduler;
        } else {
            this.scheduler = Executors.newScheduledThreadPool(2);
            ownedScheduler.set(true);
        }
    }

    public ScheduledFuture<?> startRenew(String key, String value, long expireSeconds, org.springframework.data.redis.core.StringRedisTemplate redisTemplate) throws Exception{
        long interval = Math.max(1, expireSeconds / 2);
        return scheduler.scheduleAtFixedRate(() -> {
            try {
                redisTemplate.execute(renewScript, Collections.singletonList(key), value, String.valueOf(expireSeconds));
            } catch (Exception e) {
                // 异常释放
                // redisTemplate.execute(releaseScript, java.util.Collections.singletonList(key), value);
                e.getMessage();
            }
        }, interval, interval, TimeUnit.SECONDS);
    }

    public void shutdownIfOwned() {
        if (ownedScheduler.get()) {
            scheduler.shutdownNow();
        }
    }
}
