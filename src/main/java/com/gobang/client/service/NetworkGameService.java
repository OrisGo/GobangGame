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

    public NetworkGameService(NetClient netClient, Piece myColor) {
        this.netClient = netClient;
        this.myColor = myColor;
    }

    @Override
    public boolean requestMove(int row, int col, Piece color) {
        if (color != myColor) return false;
        netClient.sendMessage(new Message(MessageType.MOVE, new Move(row, col, color)));
        return true;
    }

    @Override public void requestUndo() { netClient.sendMessage(new Message(MessageType.REGRET_REQUEST, "请求悔棋")); }
    @Override public void requestReset() { netClient.sendMessage(new Message(MessageType.RESET_REQUEST, "重置游戏")); }
    @Override public void sendChat(String msg) { netClient.sendMessage(new Message(MessageType.CHAT, msg)); }
    @Override public void surrender() { netClient.sendMessage(new Message(MessageType.SURRENDER, "认输")); }
}