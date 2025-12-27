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


    // 本地对决测试
    private static Game sharedGame; // 共享游戏实例

    @FXML
    public void initializeLocal() {
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
    public void initializePVE(Piece playerColor) {
        // 初始化棋盘
        chessCanvas = new ChessCanvas();
        chessCanvas.initBoard(600);
        boardContainer.getChildren().addFirst(chessCanvas);
        chessCanvas.setOnMouseClicked(this::handleBoardClick);

        // 创建游戏实例
        this.game = new Game();

        // 创建玩家
        LocalPlayer humanPlayer = new LocalPlayer("玩家", playerColor);
        AIPlayer aiPlayer = new AIPlayer("AI", playerColor.getOpposite());

        // 设置游戏玩家
        game.setPlayers(
                playerColor == Piece.BLACK ? humanPlayer : aiPlayer,
                playerColor == Piece.WHITE ? humanPlayer : aiPlayer
        );

        // 初始化 PVE 服务
        this.gameService = new PVEGameService(game);
        game.setListener(this);

        // 重置游戏开始
        game.reset();
        appendChatMessage("[系统]: 人机对战已准备就绪，黑方先行。");
        appendChatMessage("[系统]: 您使用的是" + playerColor.getName());
    }

    /**
     * 创建游戏实例并建立连通逻辑
     */
    public void startLocalGame() {
        // 创建核心逻辑实例
        this.game = new Game();

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
        int row = chessCanvas.getRowByY(event.getY());
        int col = chessCanvas.getColByX(event.getX());

        if (gameService != null) {
            // 由 Service 判断当前该谁下
            gameService.requestMove(row, col, null);
        }
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
        if (gameService != null) {
            gameService.requestReset();
        }
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
        // 1. 初始化棋盘画布，固定尺寸适配窗口
        chessCanvas = new ChessCanvas();
        chessCanvas.initBoard(600);
        boardContainer.getChildren().addFirst(chessCanvas);

        // 2. 创建游戏核心实例，绑定监听器
        game = new Game();
        game.setListener(this);

        // 3. 自动分配玩家&AI阵营：玩家选什么色，AI就用对立色
        Piece aiColor = playerColor.getOpposite();
        LocalPlayer humanPlayer = new LocalPlayer("玩家", playerColor);
        AIPlayer aiPlayer = new AIPlayer("AI", aiColor);

        // 4. 设置游戏玩家顺序（黑方先行，符合五子棋规则）
        if (playerColor == Piece.BLACK) {
            game.setPlayers(humanPlayer,aiPlayer);
        } else {
            game.setPlayers(aiPlayer,humanPlayer);
        }

        // 5. 重置游戏
        game.reset();
    }
}
