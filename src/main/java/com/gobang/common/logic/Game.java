package com.gobang.common.logic;

import com.gobang.client.player.Player;
import com.gobang.common.model.Move;
import com.gobang.common.model.Piece;
import com.gobang.common.model.GameState;
import java.util.Stack;


/**
 * 游戏逻辑类，负责维护棋盘状态和规则判定
 */

public class Game {
    public static final int BOARD_SIZE = 15;
    private final Piece[][] board;
    private Piece currentTurn;
    private GameState state;
    private final Stack<Move> moveHistory;

    // 监听器引用，通知UI更新
    private GameListener listener;
    // 玩家
    public Player playerBlack;
    public Player playerWhite;


    public Game()
    {
        this.board = new Piece[BOARD_SIZE][BOARD_SIZE];
        this.moveHistory = new Stack<>();
        reset();
    }

    public  void setPlayers(Player black, Player white)
    {
        this.playerBlack = black;
        this.playerWhite = white;
    }
    public void setListener(GameListener listener) {
        this.listener = listener;
    }

    /**
     * 核心方法：落子
     * @return boolean 是否落子成功
     */
    public synchronized boolean placePiece(int row, int col, Piece color) {
        // 1. 基础校验：游戏未开始、坐标越界、位置已有棋子、非当前回合玩家
        if (state != GameState.PLAYING) return false;
        if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) return false;
        if (board[row][col] != Piece.EMPTY) return false;
        if (color != currentTurn) return false;

        // 2. 执行落子
        board[row][col] = color;
        Move move = new Move(row, col, color);
        moveHistory.push(move);

        // 3. 通知 UI 绘制
        if (listener != null) listener.onChessPlaced(row, col, color);

        // 4. 胜负判断
        if (checkWin(row, col, color)) {
            state = GameState.FINISHED;
            if (listener != null) listener.onGameOver(color);
        } else if(isBoardFull()) {
            state = GameState.FINISHED;
            if (listener != null) listener.onGameOver(Piece.EMPTY);
        } else {
            // 5. 切换回合
            currentTurn = (currentTurn == Piece.BLACK) ? Piece.WHITE : Piece.BLACK;
            if (listener != null) listener.onTurnChanged(currentTurn);

            triggerNextTurn();
        }

        return true;
    }

    private void triggerNextTurn() {
        Player nextPlayer = (currentTurn == Piece.BLACK) ? playerBlack : playerWhite;
        if (nextPlayer != null) {
            nextPlayer.onTurn(this);
        }
    }

    /**
     * 五子连珠判定算法（基于你原有的逻辑优化）
     */
    private boolean checkWin(int row, int col, Piece color) {
        int[][] directions = {{0,1}, {1,0}, {1,1}, {1,-1}};
        for (int[] dir : directions) {
            int count = 1;
            // 正向找
            count += countInDirection(row, col, dir[0], dir[1], color);
            // 反向找
            count += countInDirection(row, col, -dir[0], -dir[1], color);
            if (count >= 5) return true;
        }
        return false;
    }

    private int countInDirection(int row, int col, int dRow, int dCol, Piece color) {
        int count = 0;
        int r = row + dRow;
        int c = col + dCol;
        while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == color) {
            count++;
            r += dRow;
            c += dCol;
        }
        return count;
    }

    public void reset() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) board[i][j] = Piece.EMPTY;
        }
        moveHistory.clear();
        currentTurn = Piece.BLACK; // 黑棋先行
        state = GameState.PLAYING;
        if (listener != null) listener.onGameReset();
        triggerNextTurn();
    }

    /**
     * 悔棋逻辑
     */
    public synchronized void undo() {
        if (state != GameState.PLAYING || this.moveHistory == null || moveHistory.isEmpty()) return;

        Move lastMove = moveHistory.pop();
        board[lastMove.row()][lastMove.col()] = Piece.EMPTY;
        currentTurn = lastMove.color(); // 回合退回给下棋的人

        if (listener != null) {
            listener.onUndo(lastMove.row(), lastMove.col());
            listener.onRedrawAll();
            listener.onTurnChanged(currentTurn);
        }
    }

    private boolean isBoardFull() {
        return moveHistory.size() >= BOARD_SIZE * BOARD_SIZE;
    }

    // Getters 和其他工具方法
    public Piece[][] getBoard() { return board; }
    public Piece getCurrentTurn() { return currentTurn; }
    public boolean checkWinExternal(int row, int col, Piece color){
        return checkWin(row,col,color);
    }

}
