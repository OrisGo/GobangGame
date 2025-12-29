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

                // 修复点 1: 先设置监听器，GameSceneController 现在实现了该接口
                netClient.setListener(gameController);

                // 修复点 2: 传入 netClient 对象即可，不要在外面拿流
                gameController.initOnlineGame(netClient, color, roomId);

                Stage currentStage = (stage != null) ? stage : (Stage) btnConnect.getScene().getWindow();
                currentStage.setScene(new Scene(gameRoot, 800, 600));
                currentStage.setTitle("五子棋对战 - 房间: " + roomId);

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
    public void close() {
        System.out.println("关闭连接界面...");

        // 安全断开连接
        try {
            if (netClient != null && netClient.isConnected()) {
                netClient.disconnect();
            }
        } catch (Exception e) {
            System.out.println("断开连接时出错: " + e.getMessage());
        }

        Platform.runLater(() -> {
            // 关闭当前窗口
            if (stage != null) {
                stage.close();
            } else {
                Node source = btnConnect;
                Stage currentStage = (Stage) source.getScene().getWindow();
                currentStage.close();
            }
        });
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

    @Override
    public void onRegretRequest(String message) {
        Platform.runLater(() -> {
            System.out.println("[ClientConnect] 收到悔棋请求: " + message);
        });
    }

    @Override
    public void onResetRequest(String message) {
        Platform.runLater(() -> {
            System.out.println("[ClientConnect] 收到重置请求: " + message);
        });
    }

    @Override
    public void onChatReceived(String message) {
        Platform.runLater(() -> {
            System.out.println("[ClientConnect] 收到聊天: " + message);
        });
    }
}