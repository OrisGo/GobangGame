package com.gobang.client.controller;

import com.gobang.client.ui.ChessCanvas;
import com.gobang.common.logic.GameListener;
import com.gobang.common.logic.GameService;
import com.gobang.common.model.Piece;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class GameController implements GameListener {
    @FXML private StackPane boardContainer;
    @FXML private Label statusLabel;
    @FXML private Circle turnIndicator;
    @FXML private TextArea chatArea;
    @FXML private TextField messageInput;

    private ChessCanvas chessCanvas;
    private GameService gameService;

    public void initialize() {
        // 初始化棋盘绘制组件
        chessCanvas = new ChessCanvas();
        chessCanvas.initBoard(600);
        boardContainer.getChildren().addFirst(chessCanvas); // 确保在底层绘制
    }

    public void setGameService(GameService service) {
        this.gameService = service;
    }

    /** 捕捉点击：UI -> Service */
    @FXML
    public void handleBoardClick(MouseEvent event) {
        int row = chessCanvas.getRowByY(event.getY());
        int col = chessCanvas.getColByX(event.getX());

        if (gameService != null) {
            // 注意：Piece 由 Service 或 Game 内部判断当前该谁下，此处传 null 或约定值
            gameService.requestMove(row, col, null);
        }
    }

    /** 发送消息交互 */
    @FXML
    public void handleSendMessage() {
        String msg = messageInput.getText();
        if (msg != null && !msg.trim().isEmpty()) {
            if (gameService != null) {
                gameService.sendChat(msg);
                appendChatMessage("我: " + msg); // 本地回显
            }
            messageInput.clear();
        }
    }

    /** 控制指令转发 */
    @FXML public void handleUndo() { if (gameService != null) gameService.requestUndo(); }
    @FXML public void handleReset() { if (gameService != null) gameService.requestReset(); }

    // --- GameListener 回调接口实现：Logic -> UI ---

    @Override
    public void onChessPlaced(int row, int col, Piece color) {
        Platform.runLater(() -> chessCanvas.drawPiece(row, col, color));
    }

    @Override
    public void onTurnChanged(Piece nextTurn) {
        Platform.runLater(() -> {
            statusLabel.setText(nextTurn == Piece.BLACK ? "黑方落子" : "白方落子");
            turnIndicator.setFill(nextTurn == Piece.BLACK ? Color.BLACK : Color.WHITE);
        });
    }

    @Override
    public void onGameOver(Piece winner) {
        Platform.runLater(() -> {
            String msg = (winner == Piece.EMPTY) ? "平局！" : (winner == Piece.BLACK ? "黑方胜利！" : "白方胜利！");
            Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
            alert.setTitle("游戏结束");
            alert.showAndWait();
        });
    }

    @Override
    public void onGameReset() {
        Platform.runLater(() -> {
            chessCanvas.drawBoard();
            appendChatMessage("[系统]: 游戏已重置");
        });
    }

    @Override
    public void onUndo(int row, int col) {
        // 悔棋通常需要全盘重绘或局部擦除，此处调用全盘重绘确保准确
        Platform.runLater(() -> chessCanvas.drawBoard());
    }

    public void appendChatMessage(String message) {
        Platform.runLater(() -> chatArea.appendText(message + "\n"));
    }
}