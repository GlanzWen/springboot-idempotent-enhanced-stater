package com.glanz.idempotent.annotation;

import com.glanz.idempotent.sceneEnum.SceneEnum;

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
    String key() default "";                // SpEL，支持手动指定
    long expireSeconds() default 60;        // 对 HTTP 可小，MQ 场景建议设置大一些
    String keyPrefix() default "IDEMPOTENT:";
    SceneEnum sceneType() default SceneEnum.HTTP;
}
