package com.gobang.client.controller;


import com.gobang.client.player.AIPlayer;
import com.gobang.client.player.LocalPlayer;
import com.gobang.common.logic.Game;
import com.gobang.common.model.Piece;
import javafx.scene.layout.StackPane;
import com.gobang.client.service.PVEGameService;
import com.gobang.client.ui.ChessCanvas;
import com.gobang.common.logic.GameListener;
import com.gobang.common.logic.GameService;
import com.gobang.client.service.LocalGameService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;

public class GameSceneController implements GameListener {
    @FXML private StackPane boardContainer;
    @FXML private Label statusLabel;
    @FXML private Circle turnIndicator;
    @FXML private TextArea chatArea;
    @FXML private TextField messageInput;
    @FXML private Label timerLabel;
    @FXML private Text gameID;  // 新增：绑定游戏标题文本

    private ChessCanvas chessCanvas;
    private Game game;
    private GameService gameService;

    String currentMode = "Local";

    // 本地对决测试
    private static Game sharedGame; // 共享游戏实例

    @FXML
    public void initializeLocal() {
        currentMode = "Local";
        chessCanvas = new ChessCanvas();
        chessCanvas.initBoard(600);
        boardContainer.getChildren().addFirst(chessCanvas);

        // 确保所有窗口共享同一个游戏实例
        if (sharedGame == null) {
            sharedGame = new Game();
        }
        this.game = sharedGame;

        // 创建本地双人服务（修改LocalGameService支持双人）
        this.gameService = new LocalGameService(game);
        game.setListener(this);
        game.reset();
        appendChatMessage("[系统]: 游戏已准备就绪，黑方先行。");
    }

    @FXML
    public void initialize() {
        // 初始化棋盘绘制组件
        chessCanvas = new ChessCanvas();
        chessCanvas.initBoard(600);
        boardContainer.getChildren().addFirst(chessCanvas); // 确保在底层绘制

        // 初始化游戏
        startLocalGame();
    }

    /**
     * 初始化人机对战模式
     * @param playerColor 玩家选择的棋子颜色
     */
    public void initPVE(Game game, Piece playerColor) {
        // 1. 彻底清空容器，防止多个 Canvas 实例重叠

        this.currentMode = "PVE";
        boardContainer.getChildren().clear();

        // 2. 重新初始化唯一的棋盘组件
        this.chessCanvas = new ChessCanvas();
        this.chessCanvas.initBoard(600);

        // 3. 必须重新绑定点击事件，否则点击无效
        this.chessCanvas.setOnMouseClicked(this::handleBoardClick);

        // 4. 将唯一的画布添加到 UI
        boardContainer.getChildren().add(this.chessCanvas);

        // 5. 绑定逻辑层与服务层
        this.game = game;
        this.gameService = new PVEGameService(game);
        this.game.setListener(this); // 确保监听器指向当前控制器

        // 6. 重置游戏开始
        game.reset();

        appendChatMessage("[系统]: 人机对战已准备就绪，黑方先行。");
        appendChatMessage("[系统]: 您使用的是 " + playerColor.getName());
    }

    /**
     * 创建游戏实例并建立连通逻辑
     */
    public void startLocalGame() {
        // 创建核心逻辑实例
        this.game = new Game();

        LocalPlayer p1 = new LocalPlayer("玩家1", Piece.BLACK);
        LocalPlayer p2 = new LocalPlayer("玩家2", Piece.WHITE);

        game.setPlayers(p1, p2);
        // 创建服务层 (此处使用本地服务)
        this.gameService = new LocalGameService(game);

        // 核心连通：将棋盘控制器作为监听器传给逻辑层
        game.setListener(this);

        // 初始化游戏状态
        game.reset();
        appendChatMessage("[系统]: 游戏已准备就绪，黑方先行。");
    }

    /** 捕捉点击：UI -> Service */
    @FXML
    public void handleBoardClick(MouseEvent event) {
        if(gameService == null || game == null) return;

        int row = chessCanvas.getRowByY(event.getY());
        int col = chessCanvas.getColByX(event.getX());

        Piece currentPlayerColor = game.getCurrentTurn();
        gameService.requestMove(row, col, currentPlayerColor);
    }

    /** 发送消息交互 */
    @FXML
    public void handleSendMessage() {
        String msg = messageInput.getText();
        if (msg != null && !msg.trim().isEmpty()) {
            appendChatMessage("我: " + msg); // 本地回显

            // 如果是网络对局，则调用 service.sendChat(msg)
            if (gameService != null) {
                gameService.sendChat(msg);
            }
            messageInput.clear();
        }
    }

    /** 控制指令：悔棋 */
    @FXML
    public void handleUndo() {
        if (gameService != null) {
            gameService.requestUndo();
        }
    }

    /** 控制指令：重置游戏 */
    @FXML
    public void handleReset() {
        if ("PVE".equals(currentMode)) {
            // PVE 模式：弹出选色框
            showPVEColorChoice();
        } else if ("LOCAL".equals(currentMode)) {
            // 本地双人：直接重置
            game.reset();
            appendChatMessage("[系统]: 本地对弈重新开始。");
        } else {
            // 联机模式：通常是发送“准备”请求给服务器
            // gameService.sendReady();
        }
    }

    private void showPVEColorChoice() {
        List<String> choices = Arrays.asList("执黑 (先行)", "执白 (后行)");
        ChoiceDialog<String> dialog = new ChoiceDialog<>("执黑 (先行)", choices);
        dialog.setHeaderText("新游戏设置");
        dialog.setContentText("请选择您的棋子颜色:");

        dialog.showAndWait().ifPresent(response -> {
            Piece playerColor = response.contains("黑") ? Piece.BLACK : Piece.WHITE;

            // 关键：彻底清空容器，重新初始化 PVE 环境
            boardContainer.getChildren().clear();
            initPVEByColor(playerColor);

            // 更新模式标记
            this.currentMode = "PVE";
        });
    }

    /** 新增：退出游戏 */
    @FXML
    public void handleExit() {
        // 获取当前窗口并关闭
        Stage stage = (Stage) boardContainer.getScene().getWindow();
        stage.close();
    }

    // --- GameListener 回调接口实现：Logic -> UI ---

    @Override
    public void onChessPlaced(int row, int col, Piece color) {
        System.out.println("UI收到落子通知: " + row + "," + col + " 颜色: " + color); // 调试行
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
        // 悔棋时重绘棋盘确保准确
        chessCanvas.drawPiece(row,col, Piece.EMPTY);
    }

    @Override
    public void onRedrawAll()
    {
        chessCanvas.drawBoard();
        // 遍历棋盘所有位置，重新绘制非空棋子
        Piece[][] board = game.getBoard();
        for (int i = 0; i < Game.BOARD_SIZE; i++) {
            for (int j = 0; j < Game.BOARD_SIZE; j++) {
                Piece piece = board[i][j];
                if (piece != Piece.EMPTY) {
                    chessCanvas.drawPiece(i, j, piece);
                }
            }
        }
    }


    public void appendChatMessage(String message) {
        Platform.runLater(() -> chatArea.appendText(message + "\n"));
    }

    public void initPVEByColor(Piece playerColor) {
        this.currentMode = "PVE"; // 标记模式

        // 1. 清理并创建新画布
        boardContainer.getChildren().clear();
        chessCanvas = new ChessCanvas();
        chessCanvas.initBoard(600);
        chessCanvas.setOnMouseClicked(this::handleBoardClick);
        boardContainer.getChildren().add(chessCanvas);

        // 2. 初始化逻辑层
        game = new Game();
        game.setListener(this);

        // 3. 配置玩家和 AI
        Piece aiColor = playerColor.getOpposite();
        LocalPlayer human = new LocalPlayer("玩家", playerColor);
        AIPlayer ai = new AIPlayer("AI", aiColor);

        if (playerColor == Piece.BLACK) {
            game.setPlayers(human, ai);
        } else {
            game.setPlayers(ai, human);
        }

        // 4. 关联服务
        this.gameService = new PVEGameService(game);

        // 5. 启动
        game.reset();
    }
}
