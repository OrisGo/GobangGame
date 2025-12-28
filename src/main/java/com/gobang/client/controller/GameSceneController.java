package com.gobang.client.controller;


import com.gobang.client.network.NetClient;
import com.gobang.client.player.AIPlayer;
import com.gobang.client.player.LocalPlayer;
import com.gobang.client.service.NetworkGameService;
import com.gobang.common.logic.Game;
import com.gobang.common.model.Move;
import com.gobang.common.model.Piece;
import com.gobang.common.network.Message;
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

public class GameSceneController implements GameListener, NetClient.ClientListener {
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
    private NetClient netClient;
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
        System.out.println("[PVE初始化] 玩家颜色: " + playerColor);
        this.currentMode = "PVE";
        this.myColor = playerColor; // 记录玩家颜色

        // 1. 彻底清空容器
        boardContainer.getChildren().clear();

        // 2. 重新初始化棋盘
        this.chessCanvas = new ChessCanvas();
        this.chessCanvas.initBoard(600);
        this.chessCanvas.setOnMouseClicked(this::handleBoardClick);
        boardContainer.getChildren().add(this.chessCanvas);

        // 3. 绑定逻辑层与服务层
        this.game = game;
        this.gameService = new PVEGameService(game);
        this.game.setListener(this);

        // 4. 重置游戏开始
        game.reset();

        appendChatMessage("[系统]: 人机对战已准备就绪，黑方先行。");
        appendChatMessage("[系统]: 您使用的是 " + playerColor.getName());

        // 5. 如果是白棋，等待AI先走
        if (myColor == Piece.WHITE) {
            appendChatMessage("[系统]: AI先手，请等待AI落子");
        }

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
        if (gameService == null || game == null) return;

        // 联机模式：只有轮到自己才能下棋
        if ("ONLINE".equals(currentMode)) {
            if (!isMyTurn) {
                appendChatMessage("[系统]: 请等待对手落子");
                return;
            }
        }

        int row = chessCanvas.getRowByY(event.getY());
        int col = chessCanvas.getColByX(event.getX());

        if (row < 0 || row >= 15 || col < 0 || col >= 15) return;

        // 检查位置是否已有棋子
        if (game.getPiece(row, col) != Piece.EMPTY) {
            appendChatMessage("[系统]: 该位置已有棋子");
            return;
        }

        // 处理不同模式
        if ("ONLINE".equals(currentMode)) {
            // 联机模式：立即更新本地UI，然后发送消息
            boolean success = game.placePiece(row, col, myColor);
            if (success) {
                // 发送网络消息
                gameService.requestMove(row, col, myColor);
                // 更新回合状态
                isMyTurn = false;
                statusLabel.setText("等待对手落子...");
                turnIndicator.setFill(myColor.getOpposite() == Piece.BLACK ? Color.BLACK : Color.WHITE);
                appendChatMessage("[我]: 落子于 (" + row + "," + col + ")");
            }
        } else if ("PVE".equals(currentMode)) {
            Piece currentColor = game.getCurrentTurn();
            System.out.println("[PVE点击] 当前回合颜色: " + currentColor + ", 玩家颜色: " + myColor);

            if (currentColor != myColor) {
                appendChatMessage("[系统]: 现在是AI的回合，请等待");
                return;
            }

            gameService.requestMove(row, col, currentColor);
        } else {
            // 本地模式：使用当前回合的颜色
            Piece colorToUse = game.getCurrentTurn();
            gameService.requestMove(row, col, colorToUse);
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

            resetPVEGame(playerColor);

            this.currentMode = "PVE";
        });
    }

    private void resetPVEGame(Piece playerColor) {
        System.out.println("[重置PVE] 新游戏，玩家颜色: " + playerColor);

        // 1. 清理棋盘
        boardContainer.getChildren().clear();

        // 2. 重新初始化棋盘
        chessCanvas = new ChessCanvas();
        chessCanvas.initBoard(600);
        chessCanvas.setOnMouseClicked(this::handleBoardClick);
        boardContainer.getChildren().add(chessCanvas);

        // 3. 创建新的游戏实例
        this.game = new Game();
        this.game.setListener(this);

        // 4. 重新配置玩家和AI
        Piece aiColor = playerColor.getOpposite();
        LocalPlayer human = new LocalPlayer("玩家", playerColor);
        AIPlayer ai = new AIPlayer("AI", aiColor);

        if (playerColor == Piece.BLACK) {
            game.setPlayers(human, ai);
            System.out.println("[重置PVE] 玩家黑棋，AI白棋");
        } else {
            game.setPlayers(ai, human);
            System.out.println("[重置PVE] AI黑棋，玩家白棋");
        }

        // 5. 更新玩家颜色记录
        this.myColor = playerColor;

        // 6. 关联服务（使用新的Game实例）
        this.gameService = new PVEGameService(game);

        // 7. 重置游戏
        game.reset();

        // 8. 如果是白棋，等待AI先走
        if (myColor == Piece.WHITE) {
            appendChatMessage("[系统]: AI先手，请等待AI落子");
            // 触发AI下棋
            game.playerBlack.onTurn(game);
        }

        // 9. 重置计时器
        stopTimer();
        elapsedSeconds = 0;
        updateTimerDisplay();
        startTimer();

        appendChatMessage("[系统]: 新游戏开始，您使用" + playerColor.getName());
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


    public void initOnlineGame(NetClient netClient, String colorStr, String roomId) {
        this.netClient = netClient;
        this.currentMode = "ONLINE";
        this.myColor = colorStr.equalsIgnoreCase("BLACK") ? Piece.BLACK : Piece.WHITE;
        this.isMyTurn = (myColor == Piece.BLACK); // 黑棋先行

        // 初始化游戏逻辑
        this.game = new Game();
        this.game.setListener(this);

        // 创建网络服务（仅负责发送消息）
        this.gameService = new NetworkGameService(netClient, myColor);

        // 设置窗口可以调整大小
        Platform.runLater(() -> {
            Stage stage = (Stage) boardContainer.getScene().getWindow();
            if (stage != null) {
                stage.setResizable(true);
            }

            // 更新状态
            statusLabel.setText("房间: " + roomId + " | 你的棋子: " +
                    (myColor == Piece.BLACK ? "黑棋" : "白棋"));

            if (isMyTurn) {
                statusLabel.setText("游戏开始：轮到您落子");
                turnIndicator.setFill(myColor == Piece.BLACK ? Color.BLACK : Color.WHITE);
            } else {
                statusLabel.setText("游戏开始：等待对手落子...");
                turnIndicator.setFill(myColor.getOpposite() == Piece.BLACK ? Color.BLACK : Color.WHITE);
            }

            // 启动计时器
            startTimer();
        });
    }

    @Override
    public void onMoveReceived(Message msg) {
        if (msg.content() instanceof Move(int row, int col, Piece color)) {
            Platform.runLater(() -> {
                System.out.println("收到落子消息: " + row + "," + col + " 颜色: " + color);

                // 如果这个棋子已经存在，跳过（避免重复绘制）
                if (game.getPiece(row, col) != Piece.EMPTY) {
                    return;
                }

                // 1. 更新游戏逻辑
                boolean success = game.placePiece(row, col, color);
                System.out.println("落子结果: " + success);

                // 2. 如果落子成功，更新UI（这一步会通过onChessPlaced自动完成）

                // 3. 切换回合指示
                if ("ONLINE".equals(currentMode)) {
                    // 如果刚落子的是对手，现在轮到我了
                    if (color != myColor) {
                        isMyTurn = true;
                        statusLabel.setText("轮到您落子");
                        turnIndicator.setFill(myColor == Piece.BLACK ? Color.BLACK : Color.WHITE);
                    } else {
                        // 如果刚落子的是我，现在轮到对手
                        isMyTurn = false;
                        statusLabel.setText("等待对手落子...");
                        turnIndicator.setFill(myColor.getOpposite() == Piece.BLACK ? Color.BLACK : Color.WHITE);
                    }
                }
            });
        }
    }

    @Override
    public void onStatusUpdate(String status) {
        Platform.runLater(() -> statusLabel.setText(status));
    }

    @Override
    public void onError(String error) {
        Platform.runLater(() -> appendChatMessage("[系统错误]: " + error));
    }

    @Override
    public void onRoomJoined(String info) {
        if (info.contains("START") || info.contains("READY")) {
            Platform.runLater(() -> {
                appendChatMessage("[系统]: 双方已就位，游戏开始！");
                resetTimer();
                startTimer();
            });
        }
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
        System.out.println("处理退出请求，当前模式: " + currentMode);

        if ("ONLINE".equals(currentMode)) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("退出确认");
            alert.setHeaderText("确定要退出游戏吗？");
            alert.setContentText("退出将断开与服务器的连接");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    // 发送离开消息
                    sendExitMessage();
                    // 关闭窗口
                    closeWindow();
                }
            });
        } else {
            closeWindow();
        }
    }

    private void sendExitMessage() {
        if (gameService instanceof NetworkGameService) {
            NetworkGameService netService = (NetworkGameService) gameService;
            try {
                netService.sendChat("玩家退出游戏");
                // 发送断开连接消息
                netService.surrender(); // 认输退出
            } catch (Exception e) {
                System.out.println("发送退出消息失败: " + e.getMessage());
            }
        }
    }

    private void closeWindow() {
        // 停止计时器
        stopTimer();

        // 关闭窗口
        Stage stage = (Stage) boardContainer.getScene().getWindow();
        stage.close();

        System.out.println("游戏窗口已关闭");
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
