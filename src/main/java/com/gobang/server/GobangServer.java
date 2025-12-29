package com.gobang.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class GobangServer {
    private ServerSocket serverSocket;
    private ServerStartController controller;
    private boolean isRunning = false;
    private Thread serverThread;

    public GobangServer(ServerStartController controller) {
        this.controller = controller;
    }

    // 启动服务器
    public void startServer(int port) {
        if (isRunning) {
            return;
        }

        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                isRunning = true;
                System.out.println("五子棋服务端启动成功，监听端口：" + port);

                // 循环接收客户端连接
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("新客户端连接：" + clientSocket.getInetAddress());

                        // 创建客户端处理器
                        ClientHandler clientHandler = new ClientHandler(clientSocket, controller);

                        // 如果controller不为null，则添加客户端处理器
                        if (controller != null) {
                            controller.addClientHandler(clientHandler);
                        }

                        new Thread(clientHandler).start();

                    } catch (IOException e) {
                        if (isRunning) {
                            System.err.println("接收客户端连接异常：" + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("服务端启动失败/异常：" + e.getMessage());
                e.printStackTrace();
            } finally {
                isRunning = false;
                System.out.println("服务端停止运行");
            }
        });

        serverThread.setDaemon(true);
        serverThread.start();
    }

    // 停止服务器
    public void stopServer() {
        isRunning = false;

        isRunning = false;

        // 先停止接受新连接
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("关闭ServerSocket异常：" + e.getMessage());
            }
        }

        // 中断服务器线程
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
        }

        // 等待服务器线程结束
        if (serverThread != null) {
            try {
                serverThread.join(3000); // 等待3秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("五子棋服务端已停止");
    }

    // 检查服务器是否正在运行
    public boolean isRunning() {
        return isRunning;
    }

    // 主方法启动（可选，用于独立运行服务端）
    public static void main(String[] args) {
        int port = 8888;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("端口号格式错误，使用默认端口8888");
            }
        }

        System.out.println("启动五子棋服务端，端口：" + port);

        // 注意：命令行运行时不需要GUI控制器
        GobangServer server = new GobangServer(null);
        server.startServer(port);

        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(server::stopServer));

        // 保持主线程运行
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}