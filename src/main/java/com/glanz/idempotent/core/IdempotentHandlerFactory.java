package com.glanz.idempotent.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author zz
 * 根据 type 获取对应的 Handler，规则：bean 名称为 '{type}IdempotentHandler'。
 */
@Component
public class IdempotentHandlerFactory {

    @Resource
    private ApplicationContext ctx;

    /**
     * 策略获取相应锁实现方式
     * @param type 锁的方式
     */
    public com.glanz.idempotent.core.IdempotentHandler getHandler(String type) {
        if (type == null) {
            return null;
        }
        String beanName = type + "IdempotentHandler";
        if (ctx.containsBean(beanName)) {
            return ctx.getBean(beanName, com.glanz.idempotent.core.IdempotentHandler.class);
        }
        return null;
    }
}
