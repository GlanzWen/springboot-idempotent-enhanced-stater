package com.glanz.idempotent.handler;

import com.glanz.idempotent.core.IdempotentHandler;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * @author zz
 * 单机幂等性使用
 * 基于 MySQL 内存表的简单幂等实现：向 idempotent_record 插入主键，插入成功则认为未重复。
 */
@Component("mysqlCacheIdempotentHandler")
public class MysqlCacheIdempotentHandler implements IdempotentHandler {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /*

    CREATE TABLE memory_idempotent_record (
        id INT PRIMARY KEY
    ) ENGINE=MEMORY;

    */
    @Override
    public boolean tryAcquire(String key, long expireSeconds) throws Exception {
        try {
            LocalDateTime expire = LocalDateTime.now().plusSeconds(expireSeconds);
            jdbcTemplate.update("INSERT INTO memory_idempotent_record (id) VALUES (?, ?)", key);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void release(String key) {
        jdbcTemplate.update("DELETE FROM memory_idempotent_record WHERE id = ?", key);
    }
}
