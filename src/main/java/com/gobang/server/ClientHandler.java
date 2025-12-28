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
            System.out.println("客户端 " + userName + " 连接异常断开: " + e.getMessage());
        } finally {
            close();  // 确保在 finally块中调用close
        }
    }

    private void handleMessage(Message message) throws IOException {
        System.out.println("收到客户端消息类型: " + message.type() + ", 内容: " + message.content());

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
            case REGRET_REQUEST:
                handleRegretRequest();
                break;
            case RESET_REQUEST:
                handleResetRequest();
                break;
            case CHAT:
                if (currentRoom != null) {
                    currentRoom.broadcastMessage(this, message);
                }
                break;
            case DISCONNECT:
                // 先通知对手再关闭
                notifyOpponentDisconnect();
                close();
                break;
            default:
                sendMessage(new Message(MessageType.ERROR, "未知消息类型"));
        }
    }

    private void handleRegretRequest() throws IOException {
        if (currentRoom != null) {
            currentRoom.handleRegretRequest(this);
        }
    }

    private void notifyOpponentDisconnect() {
        if (currentRoom != null && currentRoom.isGameStarted()) {
            try {
                // 通知对手玩家离开
                if (this == currentRoom.getBlackPlayer()) {
                    ClientHandler whitePlayer = currentRoom.getWhitePlayer();
                    if (whitePlayer != null && whitePlayer.isConnected) {
                        whitePlayer.sendMessage(new Message(
                                MessageType.DISCONNECT,
                                "对手已断开连接，游戏结束"
                        ));
                    }
                } else if (this == currentRoom.getWhitePlayer()) {
                    ClientHandler blackPlayer = currentRoom.getBlackPlayer();
                    if (blackPlayer != null && blackPlayer.isConnected) {
                        blackPlayer.sendMessage(new Message(
                                MessageType.DISCONNECT,
                                "对手已断开连接，游戏结束"
                        ));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleResetRequest() throws IOException {
        if (currentRoom != null) {
            currentRoom.handleResetRequest(this);
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
        if (!isConnected) return; // 防止重复关闭

        isConnected = false;
        System.out.println("关闭客户端连接: " + userName + " (" + getClientAddress() + ")");

        // 玩家退出房间
        if (currentRoom != null) {
            // 通知对手
            notifyOpponentDisconnect();
            currentRoom.removePlayer(this);
            currentRoom = null;
        }

        // 通知控制器客户端断开
        if (controller != null) {
            controller.onClientDisconnected(this);
        }

        // 关闭流和 Socket
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("客户端连接已完全关闭: " + userName);
    }

    public String getClientAddress() {
        return clientSocket.getInetAddress().getHostAddress();
    }

    public int getClientPort() {
        return clientSocket.getPort();
    }
    public boolean isConnected() {
        return isConnected && clientSocket != null && !clientSocket.isClosed();
    }


    public ClientHandler getOpponent() {
        if (currentRoom == null) return null;
        return (this == currentRoom.getBlackPlayer()) ?
                currentRoom.getWhitePlayer() : currentRoom.getBlackPlayer();
    }
    public void setChessColor(String chessColor) { this.chessColor = chessColor; }
    public void setCurrentRoom(GameRoom currentRoom) { this.currentRoom = currentRoom; }
    public String getUserName() { return userName; }
    public String getChessColor() { return chessColor; }
}