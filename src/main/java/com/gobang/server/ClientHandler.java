package com.gobang.server;

import com.gobang.common.network.Message;
import com.gobang.common.network.MessageType;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * 客户端连接处理器
 * 负责与单个客户端进行通信
 */
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final ServerStartController serverController;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isRunning;

    public ClientHandler(Socket socket, ServerStartController controller) {
        this.clientSocket = socket;
        this.serverController = controller;
        this.isRunning = true;
    }

    @Override
    public void run() {
        try {
            // 初始化输入输出流
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

            // 发送连接成功消息
            sendMessage(new Message(MessageType.ROOM_INFO, "成功连接到服务器"));

            // 循环接收客户端消息
            while (isRunning) {
                Message message = (Message) in.readObject();
                handleMessage(message);
            }

        } catch (IOException | ClassNotFoundException e) {
            if (isRunning) {
                System.err.println("客户端通信错误: " + e.getMessage());
            }
        } finally {
            stop();
        }
    }

    /**
     * 处理客户端发送的消息
     */
    private void handleMessage(Message message) {
        switch (message.getType()) {
            case JOIN_ROOM:
                // 处理加入房间请求
                System.out.println("客户端请求加入房间: " + message.getContent());
                // 后续可以在这里实现房间分配逻辑
                break;
            case MOVE:
                // 处理落子消息，后续需要转发给其他玩家
                System.out.println("收到落子消息: " + message.getContent());
                break;
            case CHAT:
                // 处理聊天消息
                System.out.println("收到聊天消息: " + message.getContent());
                break;
            case EXIT_ROOM:
                // 处理退出房间请求
                stop();
                break;
            default:
                System.out.println("收到未知类型消息: " + message.getType());
        }
    }

    /**
     * 向客户端发送消息
     */
    public void sendMessage(Message message) throws IOException {
        if (out != null && !clientSocket.isClosed()) {
            out.writeObject(message);
            out.flush();
        }
    }

    /**
     * 停止客户端连接
     */
    public void stop() {
        isRunning = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
            // 通知服务器控制器更新状态
            serverController.onClientDisconnected(this);
        } catch (IOException e) {
            System.err.println("关闭客户端连接错误: " + e.getMessage());
        }
    }

    /**
     * 获取客户端地址
     */
    public String getClientAddress() {
        return clientSocket.getInetAddress().getHostAddress();
    }

    /**
     * 获取客户端端口
     */
    public int getClientPort() {
        return clientSocket.getPort();
    }
}
