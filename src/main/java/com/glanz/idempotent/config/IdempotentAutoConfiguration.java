package com.glanz.idempotent.config;

import com.glanz.idempotent.handler.MysqlIdempotentHandler;
import com.glanz.idempotent.handler.RedisIdempotentHandler;
import com.glanz.idempotent.handler.TokenIdempotentHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.sql.DataSource;

/**
 * @author zz
 * 自动配置：通过条件注解控制哪些 Handler 被注入。
 * - 当 classpath 中存在 StringRedisTemplate 且 idempotent.enable 为 true 时，注入 Redis handler。
 * - 当 classpath 中存在 DataSource 且 idempotent.enable 为 true 时，注入 MySQL handler。
 * - Token handler 总是注入（无第三方依赖）
 */
@Configuration
@ConditionalOnProperty(prefix = "idempotent", name = "enable", havingValue = "true", matchIfMissing = true)
public class IdempotentAutoConfiguration {

    @Bean
    @ConditionalOnClass(StringRedisTemplate.class)
    @ConditionalOnMissingBean(name = "redisIdempotentHandler")
    public RedisIdempotentHandler redisIdempotentHandler() {
        return new RedisIdempotentHandler();
    }

    @Bean
    @ConditionalOnClass(DataSource.class)
    @ConditionalOnMissingBean(name = "mysqlIdempotentHandler")
    public MysqlIdempotentHandler mysqlIdempotentHandler() {
        return new MysqlIdempotentHandler();
    }

    @Bean
    @ConditionalOnMissingBean(name = "tokenIdempotentHandler")
    public TokenIdempotentHandler tokenIdempotentHandler() {
        return new TokenIdempotentHandler();
    }
}
