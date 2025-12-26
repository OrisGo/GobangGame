package com.gobang.common.logic;

import com.gobang.common.model.Piece;

/**
 * [Interface] 游戏事件监听器
 * 用于将逻辑层的变化（落子、胜负、重置）通知给 UI 层或服务端
 */
public interface GameListener {
    /** 当有人成功落子时触发 */
    void onChessPlaced(int row, int col, Piece color);

    /** 当游戏结束产生赢家时触发 */
    void onGameOver(Piece winner);

    /** 当游戏状态切换（轮到谁了）时触发 */
    void onTurnChanged(Piece nextTurn);

    /** 当游戏被重置时触发 */
    void onGameReset();

    /** (可选) 当发生悔棋操作时触发 */
    void onUndo(int row, int col);
}