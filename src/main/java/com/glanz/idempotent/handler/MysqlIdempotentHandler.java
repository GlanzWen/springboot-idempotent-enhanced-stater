package com.glanz.idempotent.handler;

import com.glanz.idempotent.core.IdempotentHandler;
import com.glanz.idempotent.support.IdempotentContext;
import com.glanz.idempotent.support.RedisLockRenewManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import reactor.util.annotation.Nullable;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author zz
 * 基于 MySQL 的简单幂等实现：向 idempotent_record 插入主键，插入成功则认为未重复。
 * mysql 实现锁续期，但是续期依靠的是服务器内部时间，如果设置过期时间过短会出现极大的误差
 */
@Component("mysqlIdempotentHandler")
public class MysqlIdempotentHandler implements IdempotentHandler {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    @Nullable
    private ObjectProvider<ScheduledExecutorService> schedulerProvider;

    private ScheduledExecutorService provided = null;
    @PostConstruct
    public void init() {
        if (schedulerProvider != null) {
            provided = schedulerProvider.getIfAvailable();
        }
    }

    /*

    CREATE TABLE idempotent_record (
        id INT PRIMARY KEY,
        expire_time bigint
    );

    */
    @Override
    public boolean tryAcquire(String key, long expireSeconds) {
        try {
            // 加锁之前先清除所有过期的锁
            cleanExpiredLocks();
            // 加锁
            LocalDateTime expire = LocalDateTime.now().plusSeconds(expireSeconds);
            jdbcTemplate.update("INSERT INTO idempotent_record (id) VALUES (?)", key, Timestamp.valueOf(expire));
            // 加锁成功，启动新线程开始续期
            long interval = Math.max(1, expireSeconds / 2);

            provided.scheduleAtFixedRate(() -> {
                try {
                    jdbcTemplate.update("UPDATE idempotent_record SET expire_time = ? WHERE key = ?", interval, key);
                } catch (Exception e) {
                    // 异常释放
                    release(key);
                    e.getMessage();
                }
            }, interval, interval, TimeUnit.SECONDS);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void release(String key) {
        jdbcTemplate.update("DELETE FROM idempotent_record WHERE id = ?", key);
    }

    public void cleanExpiredLocks () {
        // 获取当前时间戳
        long serverCurrentTime = System.currentTimeMillis();
        // 获取数据库过期时间数量 为0跳过，
        if (jdbcTemplate.update("SELECT count(*) FROM idempotent_record WHERE expire_time <= ?", serverCurrentTime) == 0) {
            return;
        }
        // 清除所有已过期的锁
        jdbcTemplate.update("DELETE FROM idempotent_record WHERE expire_time <= ? ", serverCurrentTime);
    }

}
