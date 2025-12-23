package gobang.network;

import gobang.util.Constant;

/**
 * 网络消息类，用于客户端与服务端通信
 */
public class Message {
    private String type; // 消息类型（CHESS/CHAT/TURN等）
    private String content; // 消息内容（落子坐标/聊天文本等）
    private int sender; // 发送者（BLACK/WHITE）

    // 消息类型常量（与Constant一致）
    public static final String CHESS = Constant.CHESS;
    public static final String CHAT = Constant.CHAT;
    public static final String TURN = Constant.TURN;
    public static final String WIN = Constant.WIN;
    public static final String RESET = Constant.RESET;
    public static final String RESET_REQUEST = "reset_request"; // 请求重置（同意再来一局）
    public static final String RESET_RESPONSE = "reset_response"; // 重置状态响应（同步双方同意状态）

    public Message(String type, String content, int sender) {
        this.type = type;
        this.content = content;
        this.sender = sender;
    }

    /**
     * 序列化：将消息转为字符串（格式：类型|内容|发送者）
     * @return 序列化字符串
     */
    public String serialize() {
        return type + "|" + content + "|" + sender;
    }

    /**
     * 反序列化：将字符串转为Message对象
     * @param str 序列化字符串
     * @return Message对象（格式错误时返回null）
     */
    public static Message deserialize(String str) {
        String[] parts = str.split("\\|");
        if (parts.length != 3) {
            return null;
        }
        try {
            return new Message(parts[0], parts[1], Integer.parseInt(parts[2]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Getter方法
    public String getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public int getSender() {
        return sender;
    }
}