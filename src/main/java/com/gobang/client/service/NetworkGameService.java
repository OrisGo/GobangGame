package com.gobang.client.service;

import com.gobang.client.controller.GameSceneController;
import com.gobang.common.logic.Game;
import com.gobang.common.logic.GameListener;
import com.gobang.common.logic.GameService;
import com.gobang.common.model.Move;
import com.gobang.common.model.Piece;
import com.gobang.common.network.Message;
import com.gobang.common.network.MessageType;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class NetworkGameService implements GameService {
    private final Game game;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final GameListener listener;
    private final Piece myColor;
    private boolean isConnected = true;

    public NetworkGameService(Game game, ObjectOutputStream out, ObjectInputStream in,
                              GameListener listener, Piece myColor) {
        this.game = game;
        this.out = out;
        this.in = in;
        this.listener = listener;
        this.myColor = myColor;

        startMessageReceiver();
    }

    private void startMessageReceiver() {
        Thread receiverThread = new Thread(() -> {
            try {
                while (isConnected) {
                    Object obj = in.readObject();
                    if (obj instanceof Message) {
                        Message message = (Message) obj;
                        handleNetworkMessage(message);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("网络连接中断: " + e.getMessage());
                handleDisconnection();
            }
        });
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    @Override
    public boolean requestMove(int row, int col, Piece color) {
        // 检查是否轮到自己
        if (color != myColor) {
            return false;
        }

        // 发送落子消息给服务器
        try {
            Move move = new Move(row, col, color);
            out.writeObject(new Message(MessageType.MOVE, move));
            out.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            handleDisconnection();
            return false;
        }
    }

    private void handleNetworkMessage(Message message) {
        System.out.println("收到网络消息: " + message.type() + " - " + message.content());

        switch (message.type()) {
            case MOVE:
                // 处理对手落子
                try {
                    Move opponentMove = (Move) message.content();

                    Platform.runLater(() -> {
                        // 通知 UI绘制对手的棋子
                        if (listener instanceof GameSceneController) {
                            GameSceneController controller = (GameSceneController) listener;
                            controller.handleNetworkMove(
                                    opponentMove.row(),
                                    opponentMove.col(),
                                    opponentMove.color()
                            );
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case CHAT:
                // 处理聊天消息
                if (listener instanceof GameSceneController) {
                    GameSceneController controller = (GameSceneController) listener;
                    controller.appendChatMessage("对手: " + message.content());
                }
                break;

            case DISCONNECT:
                // 处理对手断开连接
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("对手离开");
                    alert.setHeaderText("对手已断开连接");
                    alert.setContentText("游戏将结束并返回大厅");
                    alert.showAndWait();

                    // 关闭游戏窗口
                    if (listener instanceof GameSceneController) {
                        GameSceneController controller = (GameSceneController) listener;
                        controller.handleReturnToLobby();
                    }
                });
                break;

            default:
                System.out.println("收到未知消息: " + message.type());
        }
    }

    private void handleDisconnection() {
        isConnected = false;
        Platform.runLater(() -> {
            if (listener instanceof GameSceneController) {
                GameSceneController controller = (GameSceneController) listener;
                controller.appendChatMessage("[系统]: 网络连接已断开");
            }
        });
    }

    @Override
    public void requestUndo() {
        try {
            out.writeObject(new Message(MessageType.REGRET_REQUEST, "请求悔棋"));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void requestReset() {
        try {
            out.writeObject(new Message(MessageType.RESET_REQUEST, "请求重置游戏"));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendChat(String message) {
        try {
            out.writeObject(new Message(MessageType.CHAT, message));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surrender() {
        try {
            out.writeObject(new Message(MessageType.SURRENDER, "认输"));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}