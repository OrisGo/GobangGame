package com.gobang;

import com.gobang.client.controller.GameSceneController;
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
    // 全局变量：记录玩家选择的棋子颜色，默认黑方（测试专用）
    private static Piece playerColor = Piece.BLACK;

    /**
     * 对外提供的PVE启动方法（供MainViewController调用，带玩家选色）
     */
    public static void startPVEGame(Piece selectedColor) {
        playerColor = selectedColor;
        launch();
    }

    /**
     * PVE测试入口（默认玩家执黑，无需选色，一键启动）
     */
    public static void startPVETest() {
        playerColor = Piece.BLACK;
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        // 加载游戏界面FXML
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/GameScene.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);

        // 初始化PVE专属逻辑
        GameSceneController controller = fxmlLoader.getController();
        controller.initPVEByColor(playerColor);

        // 配置PVE窗口属性
        primaryStage.setTitle("五子棋 - 人机对战【玩家：" + playerColor.getName() + "】");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false); // 固定窗口大小，适配棋盘
        primaryStage.show();
    }

    /**
     * PVE模式极简测试入口：直接运行此main方法，默认玩家执黑，跳过主界面
     */
    public static void main(String[] args) {
        startPVETest();
    }
}
