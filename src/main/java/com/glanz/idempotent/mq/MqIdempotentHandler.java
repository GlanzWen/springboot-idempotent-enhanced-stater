package com.glanz.idempotent.mq;

public interface MqIdempotentHandler {

    /**
     * 处理消息并保证幂等
     * @param message 消息内容（可为 MQ 原始对象）
     * @param consumer 真正的消费逻辑（用户实现）
     */
    void handleMessage(Object message, Runnable consumer);

    /**
     * 判断消息是否重复消费
     */
    boolean isDuplicate(String messageId);

    /**
     * 消费完成后标记消息状态
     */
    void markConsumed(String messageId);

}
