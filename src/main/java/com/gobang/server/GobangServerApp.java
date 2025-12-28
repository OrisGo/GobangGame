package com.gobang.server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * 五子棋联机对战服务端主程序
 * 独立窗口运行，负责管理服务端状态和客户端连接
 */
public class GobangServerApp extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        // 加载服务端界面FXML
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/ServerStartView.fxml"));
        Parent root = fxmlLoader.load();

        // 设置窗口属性
        primaryStage.setTitle("五子棋联机对战 - 服务端");
        primaryStage.setScene(new Scene(root, 500, 400));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
