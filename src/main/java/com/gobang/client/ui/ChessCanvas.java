package com.gobang.client.ui;

import com.gobang.common.model.Piece;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;

public class ChessCanvas extends Canvas {
    private final int BOARD_SIZE = 15;
    private double cellSize;
    private final double padding = 30.0;

    public void initBoard(double size) {
        this.setWidth(size);
        this.setHeight(size);
        this.cellSize = (size - padding * 2) / (BOARD_SIZE - 1);
        drawBoard();
    }

    public void drawBoard() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        // 1. 背景绘制 (仿 ChessBoardPanel 的木色)
        gc.setFill(Color.web("#F0DCB4"));
        gc.fillRect(0, 0, getWidth(), getHeight());

        // 2. 绘制网格线
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.0);
        for (int i = 0; i < BOARD_SIZE; i++) {
            double pos = padding + i * cellSize;
            gc.strokeLine(padding, pos, getWidth() - padding, pos); // 横线
            gc.strokeLine(pos, padding, pos, getHeight() - padding); // 竖线
        }
    }

    public void drawPiece(int row, int col, Piece color) {
        if (color == Piece.EMPTY) return;
        GraphicsContext gc = getGraphicsContext2D();
        double x = padding + col * cellSize;
        double y = padding + row * cellSize;
        double r = cellSize * 0.45;

        // 使用径向渐变提升立体感
        RadialGradient gradient = (color == Piece.BLACK)
                ? new RadialGradient(0, 0, x - r/3, y - r/3, r, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#444444")), new Stop(1, Color.BLACK))
                : new RadialGradient(0, 0, x - r/3, y - r/3, r, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE), new Stop(1, Color.web("#DCDCDC")));

        gc.setFill(gradient);
        gc.fillOval(x - r, y - r, r * 2, r * 2);
    }

    public int getRowByY(double y) { return (int) Math.round((y - padding) / cellSize); }
    public int getColByX(double x) { return (int) Math.round((x - padding) / cellSize); }
}