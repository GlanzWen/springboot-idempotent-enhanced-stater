package com.glanz.idempotent.handler;

import com.glanz.idempotent.core.IdempotentHandler;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * @author zz
 * 基于 MySQL 的简单幂等实现：向 idempotent_record 插入主键，插入成功则认为未重复。
 */
@Component("mysqlIdempotentHandler")
public class MysqlIdempotentHandler implements IdempotentHandler {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public boolean tryAcquire(String key, long expireSeconds) {
        try {
            LocalDateTime expire = LocalDateTime.now().plusSeconds(expireSeconds);
            jdbcTemplate.update("INSERT INTO idempotent_record (id, expire_time) VALUES (?, ?)", key, Timestamp.valueOf(expire));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void release(String key) {
        jdbcTemplate.update("DELETE FROM idempotent_record WHERE id = ?", key);
    }
}
