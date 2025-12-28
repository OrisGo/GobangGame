package com.gobang.client.player;

import com.gobang.common.logic.Game;
import com.gobang.common.model.Piece;
import javafx.application.Platform;

/**
 * AI玩家类：实现自动下子逻辑
 */
public class AIPlayer implements Player {
    private Piece myColor;
    private String name = "AI";

    public AIPlayer(String name, Piece color) {
        this.name = name;
        this.myColor = color;
    }

    @Override
    public Piece getColor() { return myColor; }

    @Override
    public void setColor(Piece color) { this.myColor = color; }

    @Override
    public String getName() { return name; }

    /**
     * 当轮到AI时，直接计算并下子
     */
    @Override
    public void onTurn(Game game) {
        System.out.println("[AI] 轮到AI下棋，颜色: " + myColor);

        // 开启新线程计算，防止UI卡顿
        new Thread(() -> {
            try {
                Thread.sleep(300); // 模拟思考时间
                int[] pos = calculateBestMove(game);

                // 计算完成后，回到UI线程落子
                Platform.runLater(() -> {
                    if (pos != null) {
                        System.out.println("[AI] 决定下棋位置: (" + pos[0] + "," + pos[1] + ")");
                        game.placePiece(pos[0], pos[1], myColor);
                    } else {
                        System.out.println("[AI] 无法找到下棋位置");
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private int[] calculateBestMove(Game game) {
        Piece[][] board = game.getBoard();
        Piece enemyColor = myColor.getOpposite();

        // 1. 进攻：找自己能赢的点
        int[] winPos = findWinningPos(game, myColor);
        if (winPos != null) return winPos;

        // 2. 防守：找敌人能赢的点并堵住
        int[] defendPos = findWinningPos(game, enemyColor);
        if (defendPos != null) return defendPos;

        // 3. 占据中心点
        int center = Game.BOARD_SIZE / 2;
        if (board[center][center] == Piece.EMPTY) return new int[]{center, center};

        // 4. 中心扩散搜索空位
        for (int step = 1; step <= center; step++) {
            int[][] neighbors = {
                    {center-step, center}, {center+step, center},
                    {center, center-step}, {center, center+step}
            };
            for (int[] p : neighbors) {
                if (isValid(p[0], p[1]) && board[p[0]][p[1]] == Piece.EMPTY) return p;
            }
        }

        // 5. 最后的保底遍历
        for (int i = 0; i < Game.BOARD_SIZE; i++) {
            for (int j = 0; j < Game.BOARD_SIZE; j++) {
                if (board[i][j] == Piece.EMPTY) return new int[]{i, j};
            }
        }
        return null;
    }

    private int[] findWinningPos(Game game, Piece color) {
        Piece[][] board = game.getBoard();
        for (int r = 0; r < Game.BOARD_SIZE; r++) {
            for (int c = 0; c < Game.BOARD_SIZE; c++) {
                if (board[r][c] == Piece.EMPTY) {
                    // 模拟落子
                    board[r][c] = color;
                    boolean isWin = game.checkWinExternal(r, c, color);
                    // 撤销模拟
                    board[r][c] = Piece.EMPTY;
                    if (isWin) return new int[]{r, c};
                }
            }
        }
        return null;
    }

    private boolean isValid(int r, int c) {
        return r >= 0 && r < Game.BOARD_SIZE && c >= 0 && c < Game.BOARD_SIZE;
    }
}