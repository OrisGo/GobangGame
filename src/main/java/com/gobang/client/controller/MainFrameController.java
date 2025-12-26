package com.gobang.client.controller;

import com.gobang.client.service.LocalGameService;
import com.gobang.common.logic.Game;
import com.gobang.common.logic.GameService;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class MainFrameController {
    // 通过 fx:id="gameBoard" 自动注入嵌套的 GameController 实例
    @FXML private GameController gameBoardController;

    @FXML private TextArea chatArea;
    @FXML private TextField messageInput;

    private Game game;
    private GameService gameService;

    /**
     * JavaFX 自动调用的初始化方法
     */
    @FXML
    public void initialize() {
        // 可以在此处或通过按钮触发 startLocalGame
        startLocalGame();
    }

    /**
     * 创建游戏实例并建立连通逻辑
     */
    public void startLocalGame() {
        // 1. 创建核心逻辑实例
        this.game = new Game();

        // 2. 创建服务层 (此处使用本地服务)
        // Service 决定了请求是发给本地 Game 对象还是发往远程服务器
        this.gameService = new LocalGameService(game);

        // 3. 核心连通：将服务注入给棋盘控制器，将棋盘控制器作为监听器传给逻辑层
        gameBoardController.setGameService(gameService);
        game.setListener(gameBoardController);

        // 4. 初始化游戏状态
        game.reset();
        appendChatMessage("[系统]: 游戏已准备就绪，黑方先行。");
    }

    /**
     * 处理对话交流逻辑
     */
    @FXML
    public void handleSendMessage() {
        String msg = messageInput.getText();
        if (msg != null && !msg.trim().isEmpty()) {
            // 在本地对局中，直接显示到聊天框
            appendChatMessage("本地玩家: " + msg);

            // 如果是网络对局，则调用 service.sendChat(msg)
            if (gameService != null) {
                gameService.sendChat(msg);
            }
            messageInput.clear();
        }
    }

    private void appendChatMessage(String text) {
        chatArea.appendText(text + "\n");
    }
}