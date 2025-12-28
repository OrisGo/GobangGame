package com.gobang.server;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import java.util.ArrayList;
import java.util.List;

public class ServerStartController {
    @FXML public Label lblServerStatus;
    @FXML public Button btnStopServer;
    @FXML public Button btnStartServer;
    @FXML public TextField tfServerPort;
    @FXML public ListView<String> lvConnectedClients;
    @FXML public Label lblClientCount;

    private GobangServer gobangServer;
    private final List<ClientHandler> connectedClients = new ArrayList<>();

    @FXML
    private void initialize() {
        tfServerPort.setText("8888");
        btnStopServer.setDisable(true);
    }

    /**
     * 启动服务端按钮点击事件
     */
    @FXML
    private void onStartServerClicked(ActionEvent event) {
        try {
            int port = Integer.parseInt(tfServerPort.getText().trim());

            // 创建并启动服务器
            gobangServer = new GobangServer(this);
            gobangServer.startServer(port);

            // 更新UI状态
            Platform.runLater(() -> {
                lblServerStatus.setText("服务端状态：运行中（端口：" + port + "）");
                btnStartServer.setDisable(true);
                btnStopServer.setDisable(false);
                tfServerPort.setDisable(true);
            });

            System.out.println("通过UI启动服务端，端口：" + port);

        } catch (NumberFormatException e) {
            lblServerStatus.setText("服务端状态：端口格式错误");
        } catch (Exception e) {
            lblServerStatus.setText("服务端状态：启动失败 - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 停止服务端按钮点击事件
     */
    @FXML
    private void onStopServerClicked(ActionEvent event) {
        if (gobangServer != null) {
            gobangServer.stopServer();
            gobangServer = null;

            // 断开所有客户端连接
            for (ClientHandler handler : connectedClients) {
                handler.close();
            }
            connectedClients.clear();

            // 更新 UI状态
            lblServerStatus.setText("服务端状态：已停止");
            btnStartServer.setDisable(false);
            btnStopServer.setDisable(true);
            tfServerPort.setDisable(false);
            lvConnectedClients.getItems().clear();
            lblClientCount.setText("当前在线：0 人");
        }
    }

    /**
     * 添加客户端处理器（由GobangServer调用）
     */
    public void addClientHandler(ClientHandler handler) {
        connectedClients.add(handler);

        Platform.runLater(() -> {
            String clientInfo = handler.getClientAddress() + ":" + handler.getClientPort();
            lvConnectedClients.getItems().add(clientInfo);
            lblClientCount.setText("当前在线：" + connectedClients.size() + " 人");
        });
    }

    /**
     * 客户端断开连接时调用
     */
    public void onClientDisconnected(ClientHandler handler) {
        connectedClients.remove(handler);
        Platform.runLater(() -> {
            String clientInfo = handler.getClientAddress() + ":" + handler.getClientPort();
            lvConnectedClients.getItems().remove(clientInfo);
            lblClientCount.setText("当前在线：" + connectedClients.size() + " 人");
        });
    }
}