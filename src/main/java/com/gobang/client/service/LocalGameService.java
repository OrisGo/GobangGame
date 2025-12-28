package com.gobang.client.service;

import com.gobang.common.logic.Game;
import com.gobang.common.logic.GameService;
import com.gobang.common.model.Piece;

public class LocalGameService implements GameService {
    private final Game game;

    public LocalGameService(Game game) {
        this.game = game;
    }

    @Override
    public boolean requestMove(int row, int col, Piece color) {
        // 如果没指定颜色，使用当前回合颜色
        if (color == null) {
            color = game.getCurrentTurn();
        }
        return game.placePiece(row, col, color);
    }

    @Override
    public void requestUndo() { game.undo(); }

    @Override
    public void requestReset() { game.reset(); }

    @Override
    public void sendChat(String message) {
        // 本地模式可直接输出到控制台或通知 MainFrame
    }

    @Override
    public void surrender() { System.out.println("本地玩家认输"); }
}