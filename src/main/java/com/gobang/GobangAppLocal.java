package com.gobang;

import com.gobang.client.controller.GameSceneController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class GobangAppLocal extends Application {
    @Override
    public void start(Stage primaryStage) throws IOException {
        openGameWindow();
    }

    private void openGameWindow() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/GameScene.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);

        Stage stage = new Stage();
        stage.setTitle("五子棋 - " + "本地对弈");
        stage.setScene(scene);

        // 如果需要区分玩家，可以在这里给控制器设置标识
        GameSceneController controller = fxmlLoader.getController();
        // 可以添加玩家标识逻辑

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}