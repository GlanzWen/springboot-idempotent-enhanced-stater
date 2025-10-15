package com.glanz.idempotent.aspect;

import com.glanz.idempotent.annotation.Idempotent;
import com.glanz.idempotent.core.IdempotentHandler;
import com.glanz.idempotent.core.IdempotentHandlerFactory;
import com.glanz.idempotent.exception.IdempotentException;
import com.glanz.idempotent.mq.MqIdempotentHandler;
import com.glanz.idempotent.mq.mqIdExtractor.MessageIdDefaultExtractor;
import com.glanz.idempotent.sceneEnum.SceneEnum;
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

    // 锁工厂，策略适配不同的锁
    @Resource
    private IdempotentHandlerFactory factory;

    // 传递上下文
    @Resource
    private ApplicationContext ctx;

    @Around("@annotation(com.glanz.idempotent.annotation.Idempotent)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取注解相关参数内容
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Idempotent anno = method.getAnnotation(Idempotent.class);
        String parsed = SpelParser.parse(anno.key(), method, joinPoint.getArgs());
        // 生成http唯一键
        String key = anno.keyPrefix() + parsed;
        // 获取当前幂等使用场景
        SceneEnum scene = anno.sceneType();
        // 获取正式幂等key
        key = !scene.equals(SceneEnum.MQ) ? key : getMessageId(scene, joinPoint.getArgs());
        // 策略获取幂等方案
        IdempotentHandler handler = factory.getHandler(anno.type());

        if (handler == null) {
            throw new IdempotentException("未找到幂等处理器: " + anno.type());
        }
        boolean ok = handler.tryAcquire(key, anno.expireSeconds());
        if (!ok) {
            throw new IdempotentException("重复请求");
        }

        try {
            return joinPoint.proceed();
        } finally {
            handler.release(key);
            // TODO 后续做成配置类型或者其他方式实现
        }
    }


    String getMessageId(SceneEnum sceneType, Object message) {
        if (sceneType == SceneEnum.MQ) {
            // 获取MessageIdExtractor唯一实现类
            try {
                // 从ctx获取mq方式中的handler，需要用户自己实现
                MqIdempotentHandler extractor = ctx.getBean(MqIdempotentHandler.class);
                return extractor.handleMessageId(message);
            } catch (Exception e) {
                // 获取用户实现失败后，走默认唯一ID生成
                log.warn("获取mq实现失败，使用默认方式计算唯一ID");
                return MessageIdDefaultExtractor.sha256(message.toString());
            }
        }
        return null;
    }
}
