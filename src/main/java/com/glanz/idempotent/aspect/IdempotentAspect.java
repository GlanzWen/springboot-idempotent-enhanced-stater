package com.glanz.idempotent.aspect;

import com.glanz.idempotent.annotation.Idempotent;
import com.glanz.idempotent.core.IdempotentHandler;
import com.glanz.idempotent.core.IdempotentHandlerFactory;
import com.glanz.idempotent.exception.IdempotentException;
import com.glanz.idempotent.mq.mqIdExtractor.MessageIdExtractor;
import com.glanz.idempotent.sceneEnum.SceneEnum;
import com.glanz.idempotent.util.SpelParser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Resource
    private IdempotentHandlerFactory factory;

    @Resource
    private ApplicationContext ctx;

    @Around("@annotation(com.glanz.idempotent.annotation.Idempotent)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Idempotent anno = method.getAnnotation(Idempotent.class);

        String parsed = SpelParser.parse(anno.key(), method, joinPoint.getArgs());
        String key = anno.keyPrefix() + parsed;
        SceneEnum scene = anno.sceneType();
        key = !scene.equals(SceneEnum.MQ) ? key : getMessageId(scene, joinPoint.getArgs());

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
            MessageIdExtractor extractor = ctx.getBean(MessageIdExtractor.class);
            if (extractor == null) {
                return new MessageIdExtractor() {
                    @Override
                    public String extractId(Object message) {
                        return sha256(message.toString());
                    }
                }.extractId(message.toString());
            }
            // 获取相应的唯一ID
            return extractor.extractId(message);
        }
        return null;
    }
}
