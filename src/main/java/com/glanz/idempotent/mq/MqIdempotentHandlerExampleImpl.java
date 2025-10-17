package com.glanz.idempotent.mq;

public class MqIdempotentHandlerExampleImpl implements MqIdempotentMessageHandler {

    // 计算获取MessageID(需唯一)，获取的Id用来做锁key
    @Override
    public String handleMessageId(Object message) {
        return "";
    }

    // 对于重复消费的消息进行处理，是抛弃标记已完成还是延迟消费==
    @Override
    public void markConsumed(Object message) {

    }
}
