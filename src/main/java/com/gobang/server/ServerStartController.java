package com.gobang.server;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerStartController {
    @FXML
    public Label lblServerStatus;
    @FXML
    public Button btnStopServer;
    @FXML
    public Button btnStartServer;
    @FXML
    public TextField tfServerPort;
    @FXML
    public ListView<String> lvConnectedClients;
    @FXML
    public Label lblClientCount;

    private ServerSocket serverSocket;
    private boolean isServerRunning = false;
    private ExecutorService clientExecutor;
    private List<ClientHandler> connectedClients = new ArrayList<>();

    @FXML
    private void initialize() {
        // 设置默认端口
        tfServerPort.setText("8888");
        // 初始状态下停止按钮不可用
        btnStopServer.setDisable(true);
    }

    /**
     * 启动服务端按钮点击事件
     */
    @FXML
    private void onStartServerClicked(ActionEvent event) {
        if (isServerRunning) {
            return;
        }

        try {
            int port = Integer.parseInt(tfServerPort.getText().trim());
            serverSocket = new ServerSocket(port);
            isServerRunning = true;
            clientExecutor = Executors.newCachedThreadPool();

            // 更新UI状态
            lblServerStatus.setText("服务端状态：运行中（端口：" + port + "）");
            btnStartServer.setDisable(true);
            btnStopServer.setDisable(false);
            tfServerPort.setDisable(true);

            // 启动线程监听客户端连接
            new Thread(this::acceptClients, "ServerAcceptThread").start();

        } catch (NumberFormatException e) {
            lblServerStatus.setText("服务端状态：端口格式错误");
        } catch (IOException e) {
            lblServerStatus.setText("服务端状态：启动失败 - " + e.getMessage());
            isServerRunning = false;
        }
    }

    /**
     * 停止服务端按钮点击事件
     */
    @FXML
    private void onStopServerClicked(ActionEvent event) {
        if (!isServerRunning) {
            return;
        }

        try {
            isServerRunning = false;
            // 关闭服务器Socket
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            // 关闭所有客户端连接
            for (ClientHandler handler : connectedClients) {
                handler.stop();
            }
            // 关闭线程池
            if (clientExecutor != null) {
                clientExecutor.shutdown();
            }

            // 更新UI状态
            lblServerStatus.setText("服务端状态：已停止");
            btnStartServer.setDisable(false);
            btnStopServer.setDisable(true);
            tfServerPort.setDisable(false);
            lvConnectedClients.getItems().clear();
            lblClientCount.setText("当前在线：0 人");
            connectedClients.clear();

        } catch (IOException e) {
            lblServerStatus.setText("服务端状态：停止失败 - " + e.getMessage());
        }
    }

    /**
     * 监听并接受客户端连接
     */
    private void acceptClients() {
        while (isServerRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                // 创建客户端处理器
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                connectedClients.add(clientHandler);

                // 提交到线程池处理
                clientExecutor.submit(clientHandler);

                // 更新客户端列表（在UI线程中执行）
                Platform.runLater(() -> {
                    String clientInfo = clientSocket.getInetAddress().getHostAddress() +
                            ":" + clientSocket.getPort();
                    lvConnectedClients.getItems().add(clientInfo);
                    lblClientCount.setText("当前在线：" + connectedClients.size() + " 人");
                });

            } catch (IOException e) {
                if (isServerRunning) {
                    Platform.runLater(() ->
                            lblServerStatus.setText("服务端状态：连接错误 - " + e.getMessage())
                    );
                }
                break;
            }
        }
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
