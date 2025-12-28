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
        boolean success = game.placePiece(row, col, color);

        if(success && game.getCurrentTurn() != null)
        {
            Player aiPlayer = (game.getCurrentTurn() == Piece.BLACK) ?
                        game.playerBlack : game.playerWhite;

            if(aiPlayer instanceof AIPlayer) {
                aiPlayer.onTurn(game);
            }
        }

        return success;
    }

    @Override
    public void requestUndo()
    {
        game.undo();
        game.undo();
    }

    @Override
    public void requestReset() { game.reset(); }

    @Override
    public void sendChat(String message) {

    }

    @Override
    public void surrender() { System.out.println("玩家认输"); }
}
