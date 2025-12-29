package com.gobang.server;

import com.gobang.common.network.Message;
import com.gobang.common.network.MessageType;
import com.gobang.server.manager.RoomManager;
import java.io.IOException;

public class GameRoom {
    private final String roomId;
    private ClientHandler blackPlayer;
    private ClientHandler whitePlayer;
    private boolean isGameStarted;
    private boolean isRoomActive = true;

    public GameRoom(String roomId) {
        this.roomId = roomId;
        this.isGameStarted = false;
    }

    public boolean addPlayer(ClientHandler clientHandler) {
        if (!isRoomActive) {
            return false;
        }

        if (blackPlayer == null) {
            this.blackPlayer = clientHandler;
            clientHandler.setChessColor("BLACK");
            clientHandler.setCurrentRoom(this);

            // 通知玩家加入成功，等待对手
            try {
                clientHandler.sendMessage(new Message(MessageType.ROOM_JOINED,
                        "ROOM_ID=" + roomId + ",COLOR=BLACK,WAITING"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        } else if (whitePlayer == null) {
            this.whitePlayer = clientHandler;
            clientHandler.setChessColor("WHITE");
            clientHandler.setCurrentRoom(this);
            startGame(); // 双人就位，自动开局
            return true;
        }
        return false;
    }

    public boolean isFull() {
        return blackPlayer != null && whitePlayer != null;
    }

    private void startGame() {
        if (!isRoomActive || blackPlayer == null || whitePlayer == null) {
            return;
        }

        this.isGameStarted = true;
        try {
            // 通知黑棋玩家
            blackPlayer.sendMessage(new Message(MessageType.GAME_START,
                    "ROOM_ID=" + roomId + ",COLOR=BLACK,ROLE=先手,OPPONENT=" + whitePlayer.getUserName()));
            // 通知白棋玩家
            whitePlayer.sendMessage(new Message(MessageType.GAME_START,
                    "ROOM_ID=" + roomId + ",COLOR=WHITE,ROLE=后手,OPPONENT=" + blackPlayer.getUserName()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void broadcastMessage(ClientHandler sender, Message msg) throws IOException {
        if (!isRoomActive) {
            return;
        }

        System.out.println("[GameRoom] 处理消息类型: " + msg.type() + ", 发送者: " +
                (sender != null ? sender.getUserName() : "null"));

        // 处理不同类型的消息
        if (msg.type() == MessageType.REGRET_REQUEST || msg.type() == MessageType.RESET_REQUEST) {
            // 悔棋和重置请求：只发送给对手
            ClientHandler opponent = getOpponent(sender);
            if (opponent != null && opponent.isConnected()) {
                opponent.sendMessage(msg);
                System.out.println("[GameRoom] 转发 " + msg.type() + " 给对手: " + opponent.getUserName());
            }
        } else if (msg.type() == MessageType.CHAT) {
            // 聊天消息：只发送给对手
            ClientHandler opponent = getOpponent(sender);
            if (opponent != null && opponent.isConnected()) {
                opponent.sendMessage(msg);
            }
        } else if (msg.type() == MessageType.REGRET_RESPONSE || msg.type() == MessageType.RESET_RESPONSE) {
            // 悔棋和重置响应：广播给双方（包括发送者自己）
            if (blackPlayer != null && blackPlayer.isConnected()) {
                blackPlayer.sendMessage(msg);
            }
            if (whitePlayer != null && whitePlayer.isConnected()) {
                whitePlayer.sendMessage(msg);
            }
            System.out.println("[GameRoom] 广播 " + msg.type() + " 给双方");
        } else if (msg.type() == MessageType.MOVE) {
            // 落子消息：广播给双方（包括发送者自己）
            if (blackPlayer != null && blackPlayer.isConnected()) {
                blackPlayer.sendMessage(msg);
            }
            if (whitePlayer != null && whitePlayer.isConnected()) {
                whitePlayer.sendMessage(msg);
            }
        }
        // 处理其他消息类型
        else if (msg.type() == MessageType.USER_INFO || msg.type() == MessageType.ROOM_INFO ||
                msg.type() == MessageType.ROOM_JOINED || msg.type() == MessageType.GAME_START ||
                msg.type() == MessageType.EXIT_ROOM) {
            // 这些消息通常不需要转发给对手
            // 如果需要发送给对手，可以在这里添加逻辑
        }
        else {
            // 默认情况下，发送给对手
            if (sender == blackPlayer && whitePlayer != null && whitePlayer.isConnected()) {
                whitePlayer.sendMessage(msg);
            } else if (sender == whitePlayer && blackPlayer != null && blackPlayer.isConnected()) {
                blackPlayer.sendMessage(msg);
            }
        }
    }


    public void handleRegretRequest(ClientHandler requester) throws IOException {
        if (!isRoomActive || !isGameStarted) {
            return;
        }

        // 向对手发送悔棋请求
        ClientHandler opponent = getOpponent(requester);
        if (opponent != null && opponent.isConnected()) {
            opponent.sendMessage(new Message(MessageType.REGRET_REQUEST,
                    requester.getUserName() + "请求悔棋"));
        }
    }

    public void handleResetRequest(ClientHandler requester) throws IOException {
        if (!isRoomActive) {
            return;
        }

        // 向对手发送重置请求
        ClientHandler opponent = getOpponent(requester);
        if (opponent != null && opponent.isConnected()) {
            opponent.sendMessage(new Message(MessageType.RESET_REQUEST,
                    requester.getUserName() + "请求新一局"));
        }
    }

    private ClientHandler getOpponent(ClientHandler player) {
        if (player == blackPlayer) {
            return whitePlayer;
        } else if (player == whitePlayer) {
            return blackPlayer;
        }
        return null;
    }

    public synchronized void removePlayer(ClientHandler player) {
        System.out.println("从房间移除玩家: " + (player != null ? player.getUserName() : "null"));

        if (player == blackPlayer) {
            blackPlayer = null;
            if (whitePlayer != null && whitePlayer.isConnected()) {
                try {
                    whitePlayer.sendMessage(new Message(MessageType.EXIT_ROOM, "对手已离开房间"));
                    // 解除白棋玩家的房间绑定
                    whitePlayer.setCurrentRoom(null);
                    whitePlayer.setChessColor(null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (player == whitePlayer) {
            whitePlayer = null;
            if (blackPlayer != null && blackPlayer.isConnected()) {
                try {
                    blackPlayer.sendMessage(new Message(MessageType.EXIT_ROOM, "对手已离开房间"));
                    // 解除黑棋玩家的房间绑定
                    blackPlayer.setCurrentRoom(null);
                    blackPlayer.setChessColor(null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // 重置游戏状态
        this.isGameStarted = false;

        // 房间无玩家，自动销毁
        if (blackPlayer == null && whitePlayer == null) {
            isRoomActive = false;
            RoomManager.getInstance().removeRoom(this.roomId);
            System.out.println("房间 " + roomId + " 已销毁");
        }
    }

    // Getters
    public String getRoomId() { return roomId; }
    public boolean isGameStarted() { return isGameStarted; }
    public ClientHandler getBlackPlayer() { return blackPlayer; }
    public ClientHandler getWhitePlayer() { return whitePlayer; }
    public boolean isRoomActive() { return isRoomActive; }
}