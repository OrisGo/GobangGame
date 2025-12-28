package com.gobang.server;

import com.gobang.common.network.Message;
import com.gobang.common.network.MessageType;
import com.gobang.server.manager.RoomManager;
import java.io.IOException;

// 房间实体：每个房间支持2人对战，管理房间内玩家、游戏状态
public class GameRoom {
    private final String roomId; // 房间号（6位UUID）
    private ClientHandler blackPlayer; // 类型：handler包下的ClientHandler
    private ClientHandler whitePlayer; // 类型：handler包下的ClientHandler
    private boolean isGameStarted; // 游戏是否开始

    public GameRoom(String roomId) {
        this.roomId = roomId;
        this.isGameStarted = false;
    }

    // ✅ 核心修复：参数为 handler包下的 ClientHandler
    public boolean addPlayer(ClientHandler clientHandler) {
        if (blackPlayer == null) {
            this.blackPlayer = clientHandler;
            clientHandler.setChessColor("BLACK"); // 分配黑棋（先手）
            clientHandler.setCurrentRoom(this);  // 绑定玩家与房间
            return true;
        } else if (whitePlayer == null) {
            this.whitePlayer = clientHandler;
            clientHandler.setChessColor("WHITE"); // 分配白棋（后手）
            clientHandler.setCurrentRoom(this);   // 绑定玩家与房间
            startGame(); // 双人就位，自动开局
            return true;
        }
        return false; // 房间已满
    }

    // ========== 核心判断：房间是否已满（2人） ==========
    public boolean isFull() {
        return blackPlayer != null && whitePlayer != null;
    }

    // ========== 游戏开始：通知双方玩家分配棋子颜色 ==========
    private void startGame() {
        this.isGameStarted = true;
        try {
            // 通知黑棋玩家
            blackPlayer.sendMessage(new Message(MessageType.JOIN_ROOM,
                    "ROOM_ID=" + roomId + ",COLOR=BLACK,ROLE=先手"));
            // 通知白棋玩家
            whitePlayer.sendMessage(new Message(MessageType.JOIN_ROOM,
                    "ROOM_ID=" + roomId + ",COLOR=WHITE,ROLE=后手"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ✅ 核心修复：参数为 handler包下的 ClientHandler
    public void broadcastMove(ClientHandler sender, Message msg) throws IOException {
        if (sender == blackPlayer && whitePlayer != null) {
            whitePlayer.sendMessage(msg); // 黑棋落子，转发给白棋
        } else if (sender == whitePlayer && blackPlayer != null) {
            blackPlayer.sendMessage(msg); // 白棋落子，转发给黑棋
        }
    }

    // Getter
    public String getRoomId() { return roomId; }
    public boolean isGameStarted() { return isGameStarted; }

    public synchronized void removePlayer(ClientHandler player) {
        if (player == blackPlayer) {
            blackPlayer = null;
            // 通知白棋玩家对手离开
            if (whitePlayer != null) {
                try {
                    whitePlayer.sendMessage(new Message(MessageType.DISCONNECT, "对手已离开房间"));
                    // 也移除白棋玩家
                    whitePlayer.setCurrentRoom(null);
                    whitePlayer.setChessColor(null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (player == whitePlayer) {
            whitePlayer = null;
            // 通知黑棋玩家对手离开
            if (blackPlayer != null) {
                try {
                    blackPlayer.sendMessage(new Message(MessageType.DISCONNECT, "对手已离开房间"));
                    // 也移除黑棋玩家
                    blackPlayer.setCurrentRoom(null);
                    blackPlayer.setChessColor(null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // 房间无玩家，自动销毁
        if (blackPlayer == null && whitePlayer == null) {
            RoomManager.getInstance().removeRoom(this.roomId);
        } else {
            // 还有玩家在房间，游戏结束，房间标记为空闲
            this.isGameStarted = false;
        }
    }
}
