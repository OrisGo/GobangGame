package com.gobang;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * 五子棋游戏主程序入口
 * 统一启动主菜单界面
 */
public class GobangMainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/startView.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("五子棋游戏");
        primaryStage.setScene(new Scene(root, 660, 460));
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}