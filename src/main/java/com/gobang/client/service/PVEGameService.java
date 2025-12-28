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
        System.out.println("[PVE] 请求落子: (" + row + "," + col + "), 颜色: " + color);


        boolean success = game.placePiece(row, col, color);
        System.out.println("[PVE] 落子结果: " + success);

        if (success) {
            Piece winner = game.getCurrentTurn(); // 这里需要修改，应该检查是否有获胜者
            // 实际上 placePiece内部已经调用了onGameOver

            // 3. 如果游戏继续，检查下一个是否是AI
            Piece nextTurn = game.getCurrentTurn();
            System.out.println("[PVE] 下一个回合: " + nextTurn);

            // 检查下一个玩家是否是 AI
            if (nextTurn != null) {
                Player nextPlayer = (nextTurn == Piece.BLACK) ?
                        game.playerBlack : game.playerWhite;

                System.out.println("[PVE] 下一个玩家: " +
                        (nextPlayer != null ? nextPlayer.getName() : "null"));

                if (nextPlayer instanceof AIPlayer) {
                    System.out.println("[PVE] AI的回合，开始思考...");
                    // 给AI一点反应时间
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // 触发AI思考（这会自动调用AI的onTurn方法）
                    nextPlayer.onTurn(game);
                }
            }
        }

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
        game.reset();

        if (game.getCurrentTurn() != null) {
            Player currentPlayer = (game.getCurrentTurn() == Piece.BLACK) ?
                    game.playerBlack : game.playerWhite;
            if (currentPlayer instanceof AIPlayer) {
                currentPlayer.onTurn(game);
            }
        }
    }

    @Override
    public void sendChat(String message) {
        System.out.println("[PVE] 聊天: " + message);
    }

    @Override
    public void surrender() { System.out.println("玩家认输"); }
}
