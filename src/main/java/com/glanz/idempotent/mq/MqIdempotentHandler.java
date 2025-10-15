package com.glanz.idempotent.mq;

public interface MqIdempotentHandler {

    /**
     * 处理消息并保证幂等
     * @param message 消息内容（可为 MQ 原始对象）
     */
    String handleMessageId(Object message);

    /**
     * 判断消息是否重复消费
     */
    boolean isDuplicate(String messageId);

    /**
     * 消费完成后标记消息状态
     */
    void markConsumed(String messageId);

}
