package com.glanz.idempotent.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glanz.idempotent.annotation.Idempotent;
import com.glanz.idempotent.core.IdempotentHandler;
import com.glanz.idempotent.core.IdempotentHandlerFactory;
import com.glanz.idempotent.exception.IdempotentException;
import com.glanz.idempotent.mq.MqIdempotentHandler;
import com.glanz.idempotent.util.DefaultKeyExtractor;
import com.glanz.idempotent.enums.SceneEnum;
import com.glanz.idempotent.util.SpelParser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;

/**
 * @author zz
 * 拦截 @Idempotent 注解并委托对应的 Handler 来判断是否允许执行。
 */
@Aspect
@Component
public class IdempotentAspect {

    private static final Logger log = LoggerFactory.getLogger(IdempotentAspect.class);

    @Resource
    private IdempotentHandlerFactory factory;

    @Resource
    private ApplicationContext ctx;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Around("@annotation(com.glanz.idempotent.annotation.Idempotent)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        long currentThreadId = Thread.currentThread().getId();
        Idempotent anno = method.getAnnotation(Idempotent.class);

        // 通用 SpEL 解析返回 Object
        Object parsedValue = SpelParser.parseValue(anno.key(), method, joinPoint.getArgs());
        // 转换为统一字符串
        String parsed = normalizeKey(parsedValue, currentThreadId);
        // 拼接 key 前缀
        String key = anno.keyPrefix() + parsed;
        // 获取注解使用场景(默认为HTTP)
        SceneEnum scene = anno.sceneType();
        // 根据注解使用场景获取正式key
        key = !scene.equals(SceneEnum.MQ) ? key
                : getMessageId(scene, joinPoint.getArgs(), anno.handlerClass(), currentThreadId);
        // 策略获取幂等处理器
        IdempotentHandler handler = factory.getHandler(anno.type());
        if (handler == null) {
            throw new IdempotentException("未找到幂等处理器: " + anno.type());
        }
        boolean ok = handler.tryAcquire(key, anno.expireSeconds());
        if (!ok) {
            repeatHandle (joinPoint.getArgs(), scene, anno.handlerClass());
            throw new IdempotentException("重复请求");
        }

        try {
            return joinPoint.proceed();
        } finally {
            // 放锁
            handler.release(key);
        }
    }

    /**
     * 将任意对象转换为稳定的字符串
     */
    private String normalizeKey(Object value, Long currentThreadId) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        try {
            return DefaultKeyExtractor.sha256(OBJECT_MAPPER.writeValueAsString(value) + currentThreadId);
        } catch (JsonProcessingException e) {
            // 兜底 fallback
            return DefaultKeyExtractor.sha256(value.toString() + currentThreadId);
        }
    }

    String getMessageId(SceneEnum sceneType, Object message, Class handlerClass, Long currentThreadId) {
        if (sceneType == SceneEnum.MQ) {
            try {
                if (handlerClass == void.class) {
                    return DefaultKeyExtractor.sha256(normalizeKey(message, currentThreadId));
                }
                MqIdempotentHandler extractor = (MqIdempotentHandler) ctx.getBean(handlerClass);
                // 计算ID
                return extractor.handleMessageId(message);
            } catch (Exception e) {
                log.warn("获取mq实现失败，使用默认方式计算唯一ID", e);
                return DefaultKeyExtractor.sha256(normalizeKey(message, currentThreadId));
            }
        }
        return null;
    }


    void repeatHandle (Object message, SceneEnum sceneType, Class handlerClass) {
        if (sceneType == SceneEnum.MQ) {
            if (handlerClass == void.class) {
                return;
            }
            MqIdempotentHandler extractor = (MqIdempotentHandler) ctx.getBean(handlerClass);
            extractor.markConsumed(message);
        }
    }




}