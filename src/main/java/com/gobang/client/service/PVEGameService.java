package com.gobang.client.service;

import com.gobang.client.player.AIPlayer;
import com.gobang.client.player.Player;
import com.gobang.common.logic.Game;
import com.gobang.common.logic.GameService;
import com.gobang.common.model.Piece;

public class PVEGameService implements GameService {
    private final Game  game;

    public PVEGameService(Game game) {
        this.game = game;
    }

    @Override
    public boolean requestMove(int row, int col, Piece color)
    {
        System.out.println("[PVEService] 请求落子: (" + row + "," + col + "), 颜色: " + color);

        // 直接调用游戏的落子逻辑
        boolean success = game.placePiece(row, col, color);
        System.out.println("[PVEService] 落子结果: " + success);

        return success;

    }

    @Override
    public void requestUndo()
    {
        // PVE模式需要连续悔棋两次（玩家一步，AI一步）
        game.undo();
        if (game.getCurrentTurn() != null) {
            Player currentPlayer = (game.getCurrentTurn() == Piece.BLACK) ?
                    game.playerBlack : game.playerWhite;
            if (currentPlayer instanceof AIPlayer) {
                game.undo();
            }
        }
    }

    @Override
    public void requestReset() {
        System.out.println("[PVEService] 重置请求");
        game.reset();

        // 重置后，如果是AI先手，触发AI下棋
        if (game.getCurrentTurn() == Piece.BLACK && game.playerBlack != null &&
                game.playerBlack instanceof AIPlayer) {
            System.out.println("[PVEService] 重置后AI先手，触发AI下棋");
            game.playerBlack.onTurn(game);
        }
    }

    @Override
    public void sendChat(String message) {
        System.out.println("[PVE] 聊天: " + message);
    }

    @Override
    public void surrender() { System.out.println("玩家认输"); }
}
