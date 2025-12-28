package com.gobang.common.network;

import java.io.Serial;
import java.io.Serializable;

/**
 * @param type    消息类型
 * @param content 消息内容（任意序列化对象）
 */ // 网络消息实体（必须实现Serializable，支持对象序列化传输）
public record Message(MessageType type, Object content) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}