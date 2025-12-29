package com.gobang.client.network;

import com.gobang.common.network.Message;
import com.gobang.common.network.MessageType;

import javafx.application.Platform;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class NetClient {
    private final String serverIp;
    private final int serverPort;
    private final String userName;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isConnected = false;

    // 回调接口
    private ClientListener listener;

    public NetClient(String serverIp, int serverPort, String userName) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.userName = userName;
    }

    public boolean connect() {
        try {
            socket = new Socket(serverIp, serverPort);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            isConnected = true;

            // 启动消息接收线程
            Thread receiveThread = new Thread(this::receiveMessages);
            receiveThread.setDaemon(true);
            receiveThread.start();

            // 发送用户信息
            sendMessage(new Message(MessageType.USER_INFO, userName));

            return true;
        } catch (IOException e) {
            notifyError("连接失败: " + e.getMessage());
            return false;
        }
    }

    public void sendMessage(Message message) {
        try {
            if (isConnected && out != null) {
                out.writeObject(message);
                out.flush();
            }
        } catch (IOException e) {
            notifyError("发送消息失败");
            disconnect();
        }
    }

    private void receiveMessages() {
        try {
            while (isConnected) {
                Message message = (Message) in.readObject();
                handleServerMessage(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            if (isConnected) {
                notifyError("与服务器断开连接");
                disconnect();
            }
        }
    }

    private void handleServerMessage(Message message) {
        System.out.println("[NetClient] 处理服务器消息: " + message.type() + " -> " + message.content());

        if (listener != null) {
            switch (message.type()) {
                case ROOM_INFO:
                    listener.onStatusUpdate(message.content().toString());
                    break;
                case JOIN_ROOM:
                case ROOM_JOINED:
                case GAME_START:
                    listener.onRoomJoined(message.content().toString());
                    break;
                case ERROR:
                    listener.onError(message.content().toString());
                    break;
                case MOVE:
                    listener.onMoveReceived(message);
                    break;
                case REGRET_REQUEST:
                    // 处理悔棋请求
                    handleRegretRequest(message);
                    break;
                case RESET_REQUEST:
                    // 处理重置请求
                    handleResetRequest(message);
                    break;
                case REGRET_RESPONSE:
                    handleRegretResponse(message);
                    break;
                case RESET_RESPONSE:
                    handleResetResponse(message);
                    break;

                    default:
                    System.out.println("收到未知消息: " + message.type());
            }
        }
    }

    // 悔棋请求处理方法
    private void handleRegretRequest(Message message) {
        Platform.runLater(() -> {
            System.out.println("[NetClient] 处理悔棋请求: " + message.content());
            if (listener != null) {
                listener.onRegretRequest(message.content().toString());
            }
        });
    }

    // 添加重置请求处理方法
    private void handleResetRequest(Message message) {
        Platform.runLater(() -> {
            System.out.println("[NetClient] 处理重置请求: " + message.content());
            if (listener != null) {
                listener.onResetRequest(message.content().toString());
            }
        });
    }

    // 处理悔棋响应
    private void handleRegretResponse(Message message) {
        Platform.runLater(() -> {
            System.out.println("[NetClient] 处理悔棋响应: " + message.content());
            if (listener != null) {
                // 将悔棋响应传递给监听器
                listener.onMoveReceived(message);
            }
        });
    }

    // 处理重置响应
    private void handleResetResponse(Message message) {
        Platform.runLater(() -> {
            System.out.println("[NetClient] 处理重置响应: " + message.content());
            if (listener != null) {
                // 将重置响应传递给监听器
                listener.onMoveReceived(message);
            }
        });
    }

    public void disconnect() {
        if (!isConnected) return;

        isConnected = false;
        System.out.println("客户端开始断开连接...");

        try {
            // 发送断开消息
            if (out != null) {
                try {
                    out.writeObject(new Message(MessageType.DISCONNECT, "客户端主动断开"));
                    out.flush();
                } catch (Exception e) {
                    // 忽略发送失败的情况
                }
            }

            // 关闭资源
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("客户端连接已关闭");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void notifyError(String errorMsg) {
        if (listener != null) {
            listener.onError(errorMsg);
        }
    }

    public void setListener(ClientListener listener) {
        this.listener = listener;
    }

    public boolean isConnected() {
        return isConnected;
    }

    // 回调接口
    public interface ClientListener {
        void onStatusUpdate(String status);
        void onRoomJoined(String roomInfo);
        void onError(String error);
        void onMoveReceived(Message move);

        void onRegretRequest(String message);
        void onResetRequest(String message);
        void onChatReceived(String message);
    }

    public ObjectOutputStream getOutputStream() {
        return out;
    }

    public ObjectInputStream getInputStream() {
        return in;
    }
}