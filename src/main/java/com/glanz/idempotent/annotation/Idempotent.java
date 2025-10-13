package com.glanz.idempotent.annotation;

import java.lang.annotation.*;

/**
 * @author zz
 * 标注方法需要幂等保护。
 * type 可为 redis/mysql/token 或自定义 handler 类型（对应 Bean 名称 '{type}IdempotentHandler'）
 * key 支持 SpEL 表达式（需要编译时保留参数名）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {
    String type() default "redis";
    String key() default "";
    long expireSeconds() default 60;
    String keyPrefix() default "IDEMPOTENT:";
}
