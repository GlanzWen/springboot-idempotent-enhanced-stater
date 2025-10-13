package com.glanz.idempotent.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @author zz
 * 根据 type 获取对应的 Handler，规则：bean 名称为 '{type}IdempotentHandler'。
 */
@Component
public class IdempotentHandlerFactory {

    @Autowired
    private ApplicationContext ctx;

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
