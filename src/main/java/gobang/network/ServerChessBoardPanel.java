package gobang.network;

import gobang.util.Constant;

import javax.swing.*;
import java.awt.*;

/**
 * 服务端棋盘面板：实时显示双方落子
 */
public class ServerChessBoardPanel extends JPanel {
    // 棋盘数据：0-空，1-黑棋，2-白棋
    private int[][] chessBoard;

    public ServerChessBoardPanel() {
        // 初始化棋盘
        chessBoard = new int[Constant.BOARD_ROWS][Constant.BOARD_COLS];
        resetBoard();
        // 设置面板大小
        setPreferredSize(new Dimension(Constant.PORT_WIDTH, Constant.PORT_HEIGHT));
        setBackground(Color.WHITE);
    }

    /**
     * 重置棋盘
     */
    public void resetBoard() {
        for (int i = 0; i < Constant.BOARD_ROWS; i++) {
            for (int j = 0; j < Constant.BOARD_COLS; j++) {
                chessBoard[i][j] = Constant.EMPTY;
            }
        }
        repaint();
    }

    /**
     * 落子（服务端接收消息后调用）
     * @param row 行
     * @param col 列
     * @param color 颜色（1-黑，2-白）
     */
    public void placeChess(int row, int col, int color) {
        if (row >= 0 && row < Constant.BOARD_ROWS && col >= 0 && col < Constant.BOARD_COLS) {
            chessBoard[row][col] = color;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        // 抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // 新增：绘制纯色背景
        g2d.setColor(new Color(240, 220, 180));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // 1. 绘制棋盘网格
        g2d.setColor(Color.BLACK);
        // 横线
        for (int i = 0; i < Constant.BOARD_ROWS; i++) {
            int y = Constant.GRID_SIZE / 2 + i * Constant.GRID_SIZE;
            g2d.drawLine(Constant.GRID_SIZE / 2, y, Constant.PORT_WIDTH - Constant.GRID_SIZE / 2, y);
        }
        // 竖线
        for (int j = 0; j < Constant.BOARD_COLS; j++) {
            int x = Constant.GRID_SIZE / 2 + j * Constant.GRID_SIZE;
            g2d.drawLine(x, Constant.GRID_SIZE / 2, x, Constant.PORT_HEIGHT - Constant.GRID_SIZE / 2);
        }

        // 2. 绘制棋子
        for (int i = 0; i < Constant.BOARD_ROWS; i++) {
            for (int j = 0; j < Constant.BOARD_COLS; j++) {
                if (chessBoard[i][j] != Constant.EMPTY) {
                    int x = Constant.GRID_SIZE / 2 + j * Constant.GRID_SIZE - Constant.CHESS_SIZE / 2;
                    int y = Constant.GRID_SIZE / 2 + i * Constant.GRID_SIZE - Constant.CHESS_SIZE / 2;
                    if (chessBoard[i][j] == Constant.BLACK) {
                        g2d.setColor(Color.BLACK);
                        g2d.fillOval(x, y, Constant.CHESS_SIZE, Constant.CHESS_SIZE);
                    } else {
                        g2d.setColor(Color.WHITE);
                        g2d.drawOval(x, y, Constant.CHESS_SIZE, Constant.CHESS_SIZE);
                        g2d.fillOval(x, y, Constant.CHESS_SIZE, Constant.CHESS_SIZE);
                    }
                }
            }
        }
    }
}