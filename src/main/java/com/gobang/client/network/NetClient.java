package com.gobang.client.network;

import com.gobang.common.network.Message;
import com.gobang.common.network.MessageType;
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
        if (listener != null) {
            switch (message.type()) {
                case ROOM_INFO:
                    listener.onStatusUpdate(message.content().toString());
                    break;
                case JOIN_ROOM:
                    listener.onRoomJoined(message.content().toString());
                    break;
                case ERROR:
                    listener.onError(message.content().toString());
                    break;
                case MOVE:
                    listener.onMoveReceived(message);
                    break;
                default:
                    System.out.println("收到未知消息: " + message.type());
            }
        }
    }

    public void disconnect() {
        isConnected = false;
        try {
            if (out != null) {
                // 发送断开消息
                try {
                    out.writeObject(new Message(MessageType.DISCONNECT, "客户端断开"));
                    out.flush();
                } catch (Exception e) {
                    // 忽略发送失败的情况
                }
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
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
    }

    public ObjectOutputStream getOutputStream() {
        return out;
    }

    public ObjectInputStream getInputStream() {
        return in;
    }
}