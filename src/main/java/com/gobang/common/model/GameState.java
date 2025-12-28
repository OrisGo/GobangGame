package com.gobang.common.model;

/**
 * [Enum] 游戏进行状态
 */
public enum GameState {
    WAITING,    // 等待玩家加入（联机模式用）
    READY,      // 准备就绪，可以开始
    PLAYING,    // 正在对弈中
    PAUSED,     // 游戏暂停
    FINISHED    // 游戏结束（已分胜负或平局）
}