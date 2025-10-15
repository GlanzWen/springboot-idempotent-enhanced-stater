package com.glanz.idempotent.handler;

import com.glanz.idempotent.core.IdempotentHandler;
import com.glanz.idempotent.support.IdempotentContext;
import com.glanz.idempotent.support.LockRenewManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import reactor.util.annotation.Nullable;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * @author zz
 * 基于 Redis + Lua 的幂等实现。
 * - 使用 Lua 脚本执行 SET NX EX 原子操作
 * - 在内存中保存 value (UUID) 以便在 release 时进行比对并调用 release 脚本
 *
 * 注：此实现仅在 Classpath 存在 StringRedisTemplate 时被注入（由自动配置类控制）
 */
@Component("redisIdempotentHandler")
public class RedisIdempotentHandler implements IdempotentHandler {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    @Nullable
    private ObjectProvider<ScheduledExecutorService> schedulerProvider;

    private DefaultRedisScript<Long> setScript;
    private DefaultRedisScript<Long> releaseScript;
    private LockRenewManager renewManager;

    @PostConstruct
    public void init() {
        setScript = new DefaultRedisScript<>();
        setScript.setLocation(new ClassPathResource("scripts/idempotent_set.lua"));
        setScript.setResultType(Long.class);

        releaseScript = new DefaultRedisScript<>();
        releaseScript.setLocation(new ClassPathResource("scripts/idempotent_release.lua"));
        releaseScript.setResultType(Long.class);

        ScheduledExecutorService provided = null;
        if (schedulerProvider != null) {
            provided = schedulerProvider.getIfAvailable();
        }
        renewManager = new LockRenewManager(provided);
    }

    @Override
    public boolean tryAcquire(String key, long expireSeconds) throws Exception{
        String value = UUID.randomUUID().toString();
        Long r = stringRedisTemplate.execute(setScript, java.util.Collections.singletonList(key), value, String.valueOf(expireSeconds));
        if (r == 1L) {
            IdempotentContext.putVal(key, value);
            ScheduledFuture<?> future = renewManager.startRenew(key, value, expireSeconds, stringRedisTemplate);
            IdempotentContext.putRenewTask(key, future);
            return true;
        }
        return false;
    }

    @Override
    public void release(String key) {
        String value = IdempotentContext.getVal(key);
        if (value != null) {
            ScheduledFuture<?> f = IdempotentContext.getRenewTask(key);
            if (f != null) {
                f.cancel(true);
            }
            stringRedisTemplate.execute(releaseScript, java.util.Collections.singletonList(key), value);
            IdempotentContext.cleanup(key);
        }
    }
}
