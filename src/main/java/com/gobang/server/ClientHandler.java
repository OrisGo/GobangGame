package com.gobang.server;

import com.gobang.server.manager.RoomManager;
import com.gobang.common.network.Message;
import com.gobang.common.network.MessageType;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isConnected = true;
    private String userName;
    private String chessColor;
    private GameRoom currentRoom;
    private ServerStartController controller;

    public ClientHandler(Socket clientSocket, ServerStartController controller) {
        this.clientSocket = clientSocket;
        this.controller = controller;
        try {
            this.out = new ObjectOutputStream(clientSocket.getOutputStream());
            this.in = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            close();
        }
    }

    @Override
    public void run() {
        try {
            while (isConnected) {
                Message message = (Message) in.readObject();
                handleMessage(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            close();
        }
    }

    private void handleMessage(Message message) throws IOException {
        switch (message.type()) {
            case USER_INFO:
                this.userName = message.content().toString();
                sendMessage(new Message(MessageType.ROOM_INFO, "昵称已确认：" + userName));
                break;
            case JOIN_ROOM:
                handleJoinRoomRequest(message.content().toString());
                break;
            case MOVE:
                if (currentRoom != null) {
                    currentRoom.broadcastMove(this, message);
                }
                break;
            case DISCONNECT:
                close();
                break;
            default:
                sendMessage(new Message(MessageType.ERROR, "未知消息类型"));
        }
    }

    private void handleJoinRoomRequest(String reqRoomId) throws IOException {
        RoomManager roomManager = RoomManager.getInstance();
        GameRoom targetRoom;

        if ("random".equals(reqRoomId)) {
            targetRoom = roomManager.findAvailableRoom()
                    .orElseGet(roomManager::createRoom);
        } else {
            targetRoom = roomManager.getRoom(reqRoomId);
            if (targetRoom == null) {
                sendMessage(new Message(MessageType.ERROR, "错误：房间[" + reqRoomId + "]不存在！"));
                return;
            }
            if (targetRoom.isFull()) {
                sendMessage(new Message(MessageType.ERROR, "错误：房间[" + reqRoomId + "]已满！"));
                return;
            }
        }

        boolean isAddSuccess = targetRoom.addPlayer(this);
        if (!isAddSuccess) {
            sendMessage(new Message(MessageType.ERROR, "加入房间失败，房间已满"));
        }
    }

    public void sendMessage(Message message) throws IOException {
        if (out != null && isConnected) {
            out.writeObject(message);
            out.flush();
        }
    }

    public void close() {
        isConnected = false;

        // 玩家退出房间
        if (currentRoom != null) {
            currentRoom.removePlayer(this);
        }

        // 通知控制器客户端断开（只在controller不为null时）
        if (controller != null) {
            controller.onClientDisconnected(this);
        }

        // 关闭流和 Socket
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getClientAddress() {
        return clientSocket.getInetAddress().getHostAddress();
    }

    public int getClientPort() {
        return clientSocket.getPort();
    }

    public void setChessColor(String chessColor) { this.chessColor = chessColor; }
    public void setCurrentRoom(GameRoom currentRoom) { this.currentRoom = currentRoom; }
    public String getUserName() { return userName; }
    public String getChessColor() { return chessColor; }
}