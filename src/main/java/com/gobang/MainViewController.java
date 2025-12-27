package com.gobang;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import com.gobang.client.controller.GameSceneController;
import com.gobang.common.model.Piece;

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

    }

    @FXML
    void startPVEGame(ActionEvent event) {
        List<Piece> colorOptions = Arrays.asList(Piece.BLACK, Piece.WHITE);
        ChoiceDialog<Piece> colorDialog = new ChoiceDialog<>(Piece.BLACK, colorOptions);
        colorDialog.setTitle("人机对战 - 选择阵营");
        colorDialog.setHeaderText("请选择您要使用的棋子颜色");
        colorDialog.setContentText("可选：黑方（先行）/ 白方（后行）");

        // 2. 获取玩家选择结果，启动独立PVE程序
        Optional<Piece> selectedColor = colorDialog.showAndWait();
        selectedColor.ifPresent(GobangAppPVE::startPVEGame);
    }

    private void openPVEGameWindow(Piece playerColor) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/GameScene.fxml"));
        Parent root = fxmlLoader.load();

        // 获取游戏控制器并初始化人机模式
        GameSceneController controller = fxmlLoader.getController();
        controller.initializePVE(playerColor);

        // 设置并显示窗口
        Stage stage = new Stage();
        stage.setTitle("五子棋 - 人机对战");
        stage.setScene(new Scene(root, 800, 600));
        stage.show();
    }

    @FXML
    void startWebGame(ActionEvent event) {

    }

}
