package com.gobang.common.logic;

import com.gobang.common.model.Piece;

/**
 * [Interface] 游戏指令服务
 * UI 层通过此接口发送操作请求，而不必关心是本地逻辑还是发送网络包
 */
public interface GameService {
    /** 申请落子 */
    boolean requestMove(int row, int col, Piece color);

    /** 申请悔棋 */
    void requestUndo();

    /** 申请重新开始 */
    void requestReset();

    /** 发送聊天消息 */
    void sendChat(String message);

    /** 认输或主动离开 */
    void surrender();
}