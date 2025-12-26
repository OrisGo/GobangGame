package com.gobang.common.network;

/**
 * [Enum] 网络通信消息类型
 */
public enum MessageType {
    // 游戏操作
    MOVE,           // 落子消息
    REGRET_REQUEST, // 请求悔棋
    REGRET_RESPONSE,// 响应悔棋
    RESET_REQUEST,  // 请求重置
    SURRENDER,      // 认输

    // 房间与连接
    JOIN_ROOM,      // 加入房间
    EXIT_ROOM,      // 退出房间
    ROOM_INFO,      // 房间信息更新（如玩家进入、离开）
    ERROR,          // 错误信息

    // 交流
    CHAT            // 聊天文本
}