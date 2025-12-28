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

    public void broadcastMove(ClientHandler sender, Message msg) throws IOException {
        if (!isRoomActive || !isGameStarted) {
            return;
        }

        // 广播给两个玩家（包括发送者自己，这样双方UI都能更新）
        if (blackPlayer != null && blackPlayer.isConnected()) {
            blackPlayer.sendMessage(msg);
        }
        if (whitePlayer != null && whitePlayer.isConnected()) {
            whitePlayer.sendMessage(msg);
        }
    }

    public void broadcastMessage(ClientHandler sender, Message msg) throws IOException {
        if (!isRoomActive) {
            return;
        }

        if (sender == blackPlayer && whitePlayer != null && whitePlayer.isConnected()) {
            whitePlayer.sendMessage(msg);
        } else if (sender == whitePlayer && blackPlayer != null && blackPlayer.isConnected()) {
            blackPlayer.sendMessage(msg);
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