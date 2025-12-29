package com.gobang.common.network;

/**
 * [Enum] 网络通信消息类型
 */
public enum MessageType {
    // 游戏操作
    GAME_START,     // 两人到齐，服务端宣布开始
    MOVE,           // 落子消息
    REGRET_REQUEST, // 请求悔棋
    REGRET_RESPONSE,// 响应悔棋
    RESET_REQUEST,  // 请求重置
    RESET_RESPONSE, // 响应重置
    SURRENDER,      // 认输

    // 房间与连接
    CREATE_ROOM,    // 创建房间
    JOIN_ROOM,      // 加入房间
    USER_INFO,      // 客户端发送昵称
    DISCONNECT,     // 断开连接
    ROOM_JOINED,    // 服务端确认加入房间成功
    EXIT_ROOM,      // 退出房间
    ROOM_INFO,      // 房间信息更新（如玩家进入、离开）

    // 交流
    CHAT,           // 聊天文本
    ERROR,          // 错误信息


}