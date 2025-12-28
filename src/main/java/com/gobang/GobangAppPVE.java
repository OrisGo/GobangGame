package com.gobang;

import com.gobang.client.controller.GameSceneController;
import com.gobang.client.player.AIPlayer;
import com.gobang.client.player.LocalPlayer;
import com.gobang.common.logic.Game;
import com.gobang.common.model.Piece;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * 五子棋PVE模式专属主类
 * 职责：1.独立启动人机对战 2.提供PVE测试入口 3.统一管理PVE窗口配置
 * 直接运行本类main方法 → 默认玩家执黑，一键测试人机对战
 */
public class GobangAppPVE extends Application {
    private static Piece playerColor;

    // 供外部调用的启动方法
    public static void startPVEGame(Piece selectedColor) {
        playerColor = selectedColor;
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/GameScene.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);

        GameSceneController controller = fxmlLoader.getController();
        // 在这里创建玩家，实现解耦
        Game game = new Game();
        LocalPlayer humanPlayer = new LocalPlayer("玩家", playerColor);
        AIPlayer aiPlayer = new AIPlayer("AI", playerColor.getOpposite());

        // 设置玩家（根据颜色决定先后手）
        if (playerColor == Piece.BLACK) {
            game.setPlayers(humanPlayer, aiPlayer);
        } else {
            game.setPlayers(aiPlayer, humanPlayer);
        }

        // 初始化控制器
        controller.initPVE(game, playerColor);

        primaryStage.setTitle("五子棋 - 人机对战【玩家：" + playerColor.getName() + "】");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // 默认玩家执黑
        startPVEGame(Piece.BLACK);
    }
}
