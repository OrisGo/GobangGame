package com.gobang.client.controller;

import com.gobang.client.network.NetClient;
import com.gobang.common.network.Message;
import com.gobang.common.network.MessageType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ClientConnectController implements NetClient.ClientListener {
    @FXML private TextField tfServerIp;
    @FXML private TextField tfServerPort;
    @FXML private TextField tfUserName;
    @FXML private TextField tfRoomId;
    @FXML private Button btnConnect;
    @FXML private Button btnDisconnect;
    @FXML private Label lblConnectStatus;

    private Stage stage;
    private NetClient netClient;

    @FXML
    public void initialize() {
        btnDisconnect.setDisable(true);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void connect() {
        String serverIp = tfServerIp.getText().trim();
        String portStr = tfServerPort.getText().trim();
        String userName = tfUserName.getText().trim();
        String roomId = tfRoomId.getText().trim();

        if (userName.isEmpty()) {
            showAlert("错误", "请输入昵称");
            return;
        }

        try {
            int port = Integer.parseInt(portStr);

            // 创建 NetClient
            netClient = new NetClient(serverIp, port, userName);
            netClient.setListener(this);

            // 连接服务器
            if (netClient.connect()) {
                updateUIOnConnect();

                // 发送加入房间请求
                String joinRoomMsg = roomId.isEmpty() ? "random" : roomId;
                netClient.sendMessage(new Message(MessageType.JOIN_ROOM, joinRoomMsg));
            }
        } catch (NumberFormatException e) {
            showAlert("错误", "端口号格式不正确");
        } catch (Exception e) {
            showAlert("连接失败", "无法连接到服务器: " + e.getMessage());
        }
    }

    // NetClient 回调方法
    @Override
    public void onStatusUpdate(String status) {
        Platform.runLater(() -> {
            lblConnectStatus.setText("状态: " + status);
        });
    }

    @Override
    public void onRoomJoined(String roomInfo) {
        Platform.runLater(() -> {
            try {
                // 解析房间信息
                String[] parts = roomInfo.split(",");
                String roomId = "";
                String color = "";
                String role = "";

                for (String part : parts) {
                    if (part.startsWith("ROOM_ID=")) roomId = part.substring(8);
                    if (part.startsWith("COLOR=")) color = part.substring(6);
                    if (part.startsWith("ROLE=")) role = part.substring(5);
                }

                // 加载游戏界面
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/GameScene.fxml"));
                Parent gameRoot = loader.load();

                // 获取游戏控制器
                GameSceneController gameController = loader.getController();

                // 获取网络流
                ObjectOutputStream out = netClient.getOutputStream();
                ObjectInputStream in  = netClient.getInputStream();

                if (out == null || in == null) {
                    showAlert("错误", "无法获取网络连接流");
                    return;
                }

                // 初始化联机游戏
                gameController.initOnlineGame(out, in, color, roomId);

                // 显示游戏界面
                Stage currentStage = (stage != null) ? stage : (Stage) btnConnect.getScene().getWindow();
                Scene gameScene = new Scene(gameRoot, 800 ,600);
                currentStage.setScene(gameScene);
                currentStage.setTitle("五子棋对战 - 房间: " + roomId + " (" + color + " " + role + ")");

            } catch (Exception e) {
                e.printStackTrace();
                showAlert("错误", "进入游戏房间失败: " + e.getMessage());
            }
        });
    }

    @Override
    public void onError(String error) {
        Platform.runLater(() -> {
            lblConnectStatus.setText("错误: " + error);
            showAlert("错误", error);
        });
    }

    @Override
    public void onMoveReceived(Message move) {
        // 落子消息将在 GameSceneController 中处理
        System.out.println("收到落子消息: " + move.content());
    }

    @FXML
    private void close() {
        // 安全断开连接
        try {
            if (netClient != null && netClient.isConnected()) {
                netClient.disconnect();
            }
        } catch (Exception ignored) {
            // 忽略断开连接时的异常
        }

        updateUIOnDisconnect();

        // 直接获取当前控件的 Stage
        Node source = btnConnect; // 或者任何其他已注入的控件
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    private void updateUIOnConnect() {
        Platform.runLater(() -> {
            lblConnectStatus.setText("状态: 已连接");
            btnConnect.setDisable(true);
            btnDisconnect.setDisable(false);
            tfServerIp.setDisable(true);
            tfServerPort.setDisable(true);
            tfUserName.setDisable(true);
            tfRoomId.setDisable(true);
        });
    }

    private void updateUIOnDisconnect() {
        Platform.runLater(() -> {
            lblConnectStatus.setText("状态: 未连接");
            btnConnect.setDisable(false);
            btnDisconnect.setDisable(true);
            tfServerIp.setDisable(false);
            tfServerPort.setDisable(false);
            tfUserName.setDisable(false);
            tfRoomId.setDisable(false);
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}