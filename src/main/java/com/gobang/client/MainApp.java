package com.gobang.client;

import com.gobang.client.controller.MainFrameController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. 加载 FXML (假设你有一个主布局文件，或者直接加载 gameScene)
        // 注意：根据你的代码，MainFrameController 似乎是管理 GameController 的父级
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/gameScene.fxml"));
        Parent root = loader.load();

        // 2. 获取 Controller 并初始化游戏逻辑
        // 如果你的 gameScene.fxml 绑定的是 GameController，
        // 你可能需要调整逻辑，让 MainFrameController 能够管理它。
        Object controller = loader.getController();

        // 假设我们在 MainFrameController 中启动
        if (controller instanceof MainFrameController) {
            ((MainFrameController) controller).startLocalGame();
        }

        // 3. 设置舞台
        primaryStage.setTitle("五子棋对战 - 开发者测试版");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}