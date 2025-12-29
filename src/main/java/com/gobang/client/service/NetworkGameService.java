package com.gobang.client.service;

import com.gobang.client.network.NetClient;
import com.gobang.common.logic.GameService;
import com.gobang.common.model.Move;
import com.gobang.common.model.Piece;
import com.gobang.common.network.Message;
import com.gobang.common.network.MessageType;

public class NetworkGameService implements GameService {
    private final NetClient netClient;
    private final Piece myColor;
    private final String userName;


    public NetworkGameService(NetClient netClient, Piece myColor) {
        this.netClient = netClient;
        this.myColor = myColor;
        userName = "玩家";
    }

    @Override
    public boolean requestMove(int row, int col, Piece color) {
        if (color != myColor) return false;
        System.out.println("[NetworkService] 发送落子消息: (" + row + "," + col + "), 颜色: " + color);
        netClient.sendMessage(new Message(MessageType.MOVE, new Move(row, col, color)));
        return true;
    }

    @Override
    public void requestUndo() {
        System.out.println("[NetworkService] 发送悔棋请求");
        netClient.sendMessage(new Message(MessageType.REGRET_REQUEST, userName + "请求悔棋"));
    }


    @Override
    public void requestReset() {
        System.out.println("[NetworkService] 发送重置请求");
        netClient.sendMessage(new Message(MessageType.RESET_REQUEST, userName + "请求新一局"));
    }


    @Override
    public void sendChat(String msg) {
        System.out.println("[NetworkService] 发送聊天: " + msg);
        netClient.sendMessage(new Message(MessageType.CHAT, msg));
    }


    @Override
    public void surrender() {
        System.out.println("[NetworkService] 发送认输");
        netClient.sendMessage(new Message(MessageType.SURRENDER, "认输"));
    }
}