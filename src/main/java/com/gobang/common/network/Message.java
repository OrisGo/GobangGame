package com.gobang.common.network;

import java.io.Serializable;

/**
 * 通用网络消息包装类
 */
public class Message implements Serializable {
    private final MessageType type;
    private final Object content;

    public Message(MessageType type, Object content) {
        this.type = type;
        this.content = content;
    }

    public MessageType getType() { return type; }
    public Object getContent() { return content; }

    // 静态工厂方法方便创建常用消息
    public static Message move(int r, int c, com.gobang.common.model.Piece color) {
        return new Message(MessageType.MOVE, new com.gobang.common.model.Move(r, c, color));
    }
}