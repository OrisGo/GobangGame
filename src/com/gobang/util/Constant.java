package com.gobang.util;

/**
 * 游戏常量定义
 */
public class Constant {
    // 棋盘参数
    public static final int BOARD_ROWS = 15; // 行数
    public static final int BOARD_COLS = 15; // 列数
    public static final int GRID_SIZE = 40; // 网格大小（像素）
    public static final int CHESS_SIZE = 30; // 棋子大小（像素）

    // 棋子颜色
    public static final int EMPTY = 0; // 空
    public static final int BLACK = 1; // 黑棋
    public static final int WHITE = 2; // 白棋

    // 网络参数
    public static final int PORT = 8888; // 端口
    public static final String LOCAL_HOST = "127.0.0.1"; // 本地IP

    // 消息类型（Message类使用）
    public static final String CHESS = "chess"; // 落子消息
    public static final String CHAT = "chat"; // 聊天消息
    public static final String TURN = "turn"; // 回合消息
    public static final String WIN = "win"; // 获胜消息
    public static final String RESET = "reset"; // 重置消息
    // 窗口尺寸常量
    public static final int PORT_WIDTH = 800;   // 根据实际需求调整
    public static final int PORT_HEIGHT = 600;  // 根据实际需求调整
}