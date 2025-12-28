package com.gobang.client.player;

import com.gobang.common.logic.Game;
import com.gobang.common.model.Piece;

public interface  Player {
    /**
     * 获取玩家棋子颜色 (1-黑, 2-白)
     */
    Piece getColor();

    /**
     * 设置玩家棋子颜色
     */
    void setColor(Piece color);


    /**
     *  获取玩家名称
     */
    String getName();

    /**
     * 当轮到该玩家下棋时，Game对象会调用此方法
     * @param game 当前游戏上下文，Player可以从中读取棋盘状态
     */
    void onTurn(Game game);

}
