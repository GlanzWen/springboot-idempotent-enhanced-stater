package com.glanz.idempotent.util;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * @author zz
 * 解析注解中的 Spring Expression Language 表达式，生成幂等 key 片段。
 */
public class SpelParser {
    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final DefaultParameterNameDiscoverer DISCOVERER = new DefaultParameterNameDiscoverer();

    public static String parse(String spel, Method method, Object[] args) {
        if (spel == null || spel.trim().isEmpty()) {
            return "";
        }
        String[] params = DISCOVERER.getParameterNames(method);
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        if (params != null) {
            for (int i=0;i<params.length && i<args.length;i++) {
                ctx.setVariable(params[i], args[i]);
            }
        }
        return PARSER.parseExpression(spel).getValue(ctx, String.class);
    }
}
