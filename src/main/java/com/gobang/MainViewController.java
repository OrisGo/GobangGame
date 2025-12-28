package com.gobang;

import com.gobang.client.controller.ClientConnectController;
import com.gobang.client.controller.GameSceneController;
import com.gobang.client.player.AIPlayer;
import com.gobang.client.player.LocalPlayer;
import com.gobang.common.logic.Game;
import com.gobang.common.model.Piece;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class MainViewController {

    @FXML
    private Button ExitButton;

    @FXML
    private Button LocalMode;

    @FXML
    private Button PVEMode;

    @FXML
    private Button WebMode;

    @FXML
    void exitGame(ActionEvent event) {
        Platform.exit();
        System.exit(0);
    }

    @FXML
    void onButtonExit(MouseEvent event) {
        Button btn = (Button) event.getSource();
        btn.setStyle(
                "-fx-background-color: #3a3f45;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 15px;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-color: #5a5f66;" +
                        "-fx-cursor: hand;"
        );
    }

    @FXML
    void onButtonHover(MouseEvent event) {
        Button btn = (Button) event.getSource();
        btn.setStyle(
                "-fx-background-color: #4e88ff;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 15px;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-color: #6fa1ff;" +
                        "-fx-cursor: hand;"
        );
    }

    @FXML
    void startLocalGame(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/GameScene.fxml"));
            Parent root = fxmlLoader.load();

            Stage stage = new Stage();
            stage.setTitle("五子棋 - 本地对弈");
            stage.setScene(new Scene(root, 800, 600));

            GameSceneController controller = fxmlLoader.getController();
            controller.startLocalGame();

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void startPVEGame(ActionEvent event) {
        List<String> colorOptions = Arrays.asList("执黑（先行）", "执白（后行）");
        ChoiceDialog<String> colorDialog = new ChoiceDialog<>("执黑（先行）", colorOptions);
        colorDialog.setTitle("人机对战 - 选择阵营");
        colorDialog.setHeaderText("请选择您要使用的棋子颜色");
        colorDialog.setContentText("可选：黑方（先行）/ 白方（后行）");

        Optional<String> result = colorDialog.showAndWait();
        result.ifPresent(choice -> {
            try {
                Piece playerColor = choice.contains("黑") ? Piece.BLACK : Piece.WHITE;

                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/GameScene.fxml"));
                Parent root = fxmlLoader.load();

                Stage stage = new Stage();
                stage.setTitle("五子棋 - 人机对战");
                stage.setScene(new Scene(root, 800, 600));

                GameSceneController controller = fxmlLoader.getController();

                // 创建游戏和玩家
                Game game = new Game();
                LocalPlayer humanPlayer = new LocalPlayer("玩家", playerColor);
                AIPlayer aiPlayer = new AIPlayer("AI", playerColor.getOpposite());

                // 设置玩家
                if (playerColor == Piece.BLACK) {
                    game.setPlayers(humanPlayer, aiPlayer);
                } else {
                    game.setPlayers(aiPlayer, humanPlayer);
                }

                controller.initPVE(game, playerColor);
                stage.show();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    void startWebGame(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/ClientConnectView.fxml"));
            Parent root = fxmlLoader.load();

            Stage stage = new Stage();
            stage.setTitle("五子棋 - 联机对战");
            stage.setScene(new Scene(root, 400, 350));
            stage.setResizable(false);

            // 设置控制器的舞台引用
            ClientConnectController controller = fxmlLoader.getController();
            controller.setStage(stage);

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}