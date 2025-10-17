package com.glanz.idempotent.mq;

public class MqIdempotentHandlerExampleImpl implements MqIdempotentHandler{
    @Override
    public String handleMessageId(Object message) {
        // 计算获取MessageID(需唯一)
        return "";
    }

    @Override
    public void markConsumed(Object message) {
        // 对于重复消费的消息进行处理，是抛弃标记已完成还是延迟消费==
    }
}
