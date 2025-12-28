package com.gobang.client.controller;


import com.gobang.client.player.AIPlayer;
import com.gobang.client.player.LocalPlayer;
import com.gobang.client.service.NetworkGameService;
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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class GameSceneController implements GameListener {
    @FXML private StackPane boardContainer;
    @FXML private Label statusLabel;
    @FXML private Circle turnIndicator;
    @FXML private TextArea chatArea;
    @FXML private TextField messageInput;
    @FXML private Label timerLabel;
    @FXML private Text gameID;  // 新增：绑定游戏标题文本


    private Piece myColor = Piece.BLACK; // 玩家颜色
    private boolean isMyTurn = true; // 是否轮到自己
    private Timer gameTimer;
    private int elapsedSeconds = 0;
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

        startTimer();
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

        startTimer();
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

        startTimer();
    }

    /** 捕捉点击：UI -> Service */
    @FXML
    public void handleBoardClick(MouseEvent event) {
        if(gameService == null || game == null) return;

        // 联机模式下检查是否轮到自己
        if ("ONLINE".equals(currentMode)) {
            if (!isMyTurn) {
                appendChatMessage("[系统]: 请等待对手落子");
                return;
            }
        }

        int row = chessCanvas.getRowByY(event.getY());
        int col = chessCanvas.getColByX(event.getX());

        if (row < 0 || row >= 15 || col < 0 || col >= 15) {
            appendChatMessage("[系统]: 请点击有效的棋盘位置");
            return;
        }

        // 获取当前应该使用的颜色
        Piece colorToUse;
        if ("ONLINE".equals(currentMode)) {
            colorToUse = myColor;  // 联机模式下使用自己的颜色
        } else if ("PVE".equals(currentMode)) {
            // PVE模式下，玩家只能使用自己的颜色
            if (game.playerBlack != null && game.playerBlack.getColor() != Piece.EMPTY) {
                colorToUse = game.playerBlack.getColor();
            } else if (game.playerWhite != null && game.playerWhite.getColor() != Piece.EMPTY) {
                colorToUse = game.playerWhite.getColor();
            } else {
                colorToUse = Piece.BLACK; // 默认
            }
        } else {
            colorToUse = game.getCurrentTurn(); // 本地模式使用当前回合的颜色
        }

        // 发送落子请求
        boolean success = gameService.requestMove(row, col, colorToUse);

        if (success && "ONLINE".equals(currentMode)) {
            isMyTurn = false; // 落子后等待对手
            statusLabel.setText("等待对手落子...");
            appendChatMessage("[系统]: 已落子，等待对手...");
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
    public void handleReset() {
        if ("PVE".equals(currentMode)) {
            // PVE 模式：弹出选色框
            showPVEColorChoice();
        } else if ("Local".equals(currentMode)) {
            // 本地双人：直接重置并重新初始化
            resetLocalGame();
        } else if ("ONLINE".equals(currentMode)) {
            // 联机模式：发送重置请求
            if (gameService != null) {
                gameService.requestReset();
            }
        }
    }

    private void resetLocalGame() {
        // 清理棋盘
        boardContainer.getChildren().clear();

        // 重新初始化棋盘
        chessCanvas = new ChessCanvas();
        chessCanvas.initBoard(600);
        chessCanvas.setOnMouseClicked(this::handleBoardClick);
        boardContainer.getChildren().add(chessCanvas);

        // 重新创建游戏
        this.game = new Game();
        LocalPlayer p1 = new LocalPlayer("玩家1", Piece.BLACK);
        LocalPlayer p2 = new LocalPlayer("玩家2", Piece.WHITE);
        game.setPlayers(p1, p2);

        // 重新设置服务
        this.gameService = new LocalGameService(game);
        game.setListener(this);

        // 重置游戏
        game.reset();

        // 重置计时器
        stopTimer();
        elapsedSeconds = 0;
        updateTimerDisplay();
        startTimer();

        appendChatMessage("[系统]: 本地对弈重新开始。");
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

    // --- GameListener 回调接口实现：Logic -> UI ---

    @Override
    public void onChessPlaced(int row, int col, Piece color) {
        System.out.println("UI收到落子通知: " + row + "," + col + " 颜色: " + color); // 调试行
        Platform.runLater(() -> chessCanvas.drawPiece(row, col, color));
    }

    @Override
    public void onTurnChanged(Piece nextTurn) {
        Platform.runLater(() -> {
            if ("ONLINE".equals(currentMode)) {
                // 联机模式下，判断是否轮到自己
                isMyTurn = (nextTurn == myColor);
                if (isMyTurn) {
                    statusLabel.setText("轮到您落子");
                    turnIndicator.setFill(myColor == Piece.BLACK ? Color.BLACK : Color.WHITE);
                } else {
                    statusLabel.setText("等待对手落子...");
                    turnIndicator.setFill(myColor.getOpposite() == Piece.BLACK ? Color.BLACK : Color.WHITE);
                }
            } else {
                // 非联机模式
                statusLabel.setText(nextTurn == Piece.BLACK ? "黑方落子" : "白方落子");
                turnIndicator.setFill(nextTurn == Piece.BLACK ? Color.BLACK : Color.WHITE);
            }
        });
    }

    @Override
    public void onGameOver(Piece winner) {
        Platform.runLater(() -> {
            stopTimer(); // 停止计时

            String msg = (winner == Piece.EMPTY) ? "平局！" :
                    (winner == Piece.BLACK ? "黑方胜利！" : "白方胜利！");
            Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
            alert.setTitle("游戏结束");
            alert.showAndWait();

            // 如果是联机模式，可以选择返回大厅或重新开始
            if ("ONLINE".equals(currentMode)) {
                showOnlineGameEndOptions();
            }
        });
    }

    private void showOnlineGameEndOptions() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("游戏结束");
        alert.setHeaderText("游戏已结束");
        alert.setContentText("请选择操作：");

        ButtonType returnButton = new ButtonType("返回大厅");
        ButtonType rematchButton = new ButtonType("再来一局");
        ButtonType closeButton = new ButtonType("关闭");

        alert.getButtonTypes().setAll(returnButton, rematchButton, closeButton);

        alert.showAndWait().ifPresent(response -> {
            if (response == returnButton) {
                // 返回大厅逻辑
                handleReturnToLobby();
            } else if (response == rematchButton) {
                // 请求重新开始
                if (gameService != null) {
                    gameService.requestReset();
                }
            }
        });
    }

    public void handleReturnToLobby() {
        // 停止计时器
        stopTimer();

        // 如果是联机模式，发送离开房间消息
        if ("ONLINE".equals(currentMode) && gameService != null) {
            try {
                // 发送离开消息
                if (gameService instanceof NetworkGameService) {
                    NetworkGameService ngService = (NetworkGameService) gameService;
                    ngService.sendChat("玩家离开房间");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 关闭当前窗口，返回主菜单
        Stage stage = (Stage) boardContainer.getScene().getWindow();
        stage.close();
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


    public void initOnlineGame(ObjectOutputStream out, ObjectInputStream in,
                               String colorStr, String roomId) {
        this.currentMode = "ONLINE";
        this.myColor = "BLACK".equals(colorStr) ? Piece.BLACK : Piece.WHITE;
        this.isMyTurn = (myColor == Piece.BLACK); // 黑棋先行

        // 清空容器并初始化棋盘
        boardContainer.getChildren().clear();
        this.chessCanvas = new ChessCanvas();
        this.chessCanvas.initBoard(600);
        this.chessCanvas.setOnMouseClicked(this::handleBoardClick);
        boardContainer.getChildren().add(this.chessCanvas);

        // 创建游戏实例和服务
        this.game = new Game();
        this.gameService = new NetworkGameService(game, out, in, this, myColor);
        this.game.setListener(this);

        // 初始化玩家
        LocalPlayer localPlayer = new LocalPlayer("玩家", myColor);
        LocalPlayer remotePlayer = new LocalPlayer("对手", myColor.getOpposite());

        if (myColor == Piece.BLACK) {
            game.setPlayers(localPlayer, remotePlayer);
        } else {
            game.setPlayers(remotePlayer, localPlayer);
        }

        // 重置游戏开始
        game.reset();

        // 启动计时器
        startTimer();

        // 更新 UI
        Platform.runLater(() -> {
            Stage stage = (Stage) boardContainer.getScene().getWindow();
            stage.setTitle("五子棋联机对战 - 房间: " + roomId + " (" +
                    myColor.getName() + (isMyTurn ? ", 先手" : ", 后手") + ")");
            statusLabel.setText(isMyTurn ? "轮到您落子" : "等待对手落子");
            turnIndicator.setFill(myColor == Piece.BLACK ? Color.BLACK : Color.WHITE);
        });

        appendChatMessage("[系统]: 已进入房间 " + roomId);
        appendChatMessage("[系统]: 您执" + myColor.getName() + (isMyTurn ? "，先手" : "，后手"));
    }

    // 计时器相关
    private boolean isTimerRunning = false;

    // 在游戏开始时启动计时器
    private void startTimer() {
        stopTimer(); // 先停止之前的计时器

        elapsedSeconds = 0;
        updateTimerDisplay();

        gameTimer = new Timer();
        gameTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (isTimerRunning) {
                        elapsedSeconds++;
                        updateTimerDisplay();
                    }
                });
            }
        }, 1000, 1000); // 每秒触发一次

        isTimerRunning = true;
    }

    private void stopTimer() {
        isTimerRunning = false;
        if (gameTimer != null) {
            gameTimer.cancel();
            gameTimer = null;
        }
    }

    private void resetTimer() {
        stopTimer();
        elapsedSeconds = 0;
        updateTimerDisplay();
    }

    private void updateTimerDisplay() {
        int minutes = elapsedSeconds / 60;
        int seconds = elapsedSeconds % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }

    // 在游戏重置时调用
    @Override
    public void onGameReset() {
        Platform.runLater(() -> {
            chessCanvas.drawBoard();
            appendChatMessage("[系统]: 游戏已重置");

            // 重置并启动计时器
            resetTimer();
            startTimer();
        });
    }


    // 在退出游戏时停止计时器
    @FXML
    public void handleExit() {
        stopTimer();
        Stage stage = (Stage) boardContainer.getScene().getWindow();
        stage.close();
    }

    // 添加公共方法供外部访问
    public StackPane getBoardContainer() {
        return boardContainer;
    }

    // 处理网络传来的对手落子
    public void handleNetworkMove(int row, int col, Piece color) {
        Platform.runLater(() -> {
            // 绘制对手的棋子
            chessCanvas.drawPiece(row, col, color);

            // 更新游戏逻辑
            game.placePiece(row, col, color);

            // 联机模式下，轮到我了
            if ("ONLINE".equals(currentMode)) {
                isMyTurn = true;
                statusLabel.setText("轮到您落子");
                appendChatMessage("[系统]: 对手已落子，轮到您");
            }
        });
    }
}
