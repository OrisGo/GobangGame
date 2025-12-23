package gobang.network;

import gobang.ui.ChessBoardPanel;
import gobang.util.Constant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * 五子棋客户端
 */
public class Client {
    private Socket socket;
    private BufferedReader in;
    private OutputStreamWriter out;
    public boolean isConnected = false;
    private ChessBoardPanel chessBoardPanel;

    public Client() {}

    public Client(ChessBoardPanel panel) {
        this.chessBoardPanel = panel;
    }

    /**
     * 连接服务端
     * @param ip 服务端IP地址
     * @return 是否连接成功
     */
    public boolean connect(String ip) {
        try {
            socket = new Socket(ip, Constant.PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new OutputStreamWriter(socket.getOutputStream());
            isConnected = true;

            // 启动消息接收线程
            startReceiveThread();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 启动消息接收线程
     */
    private void startReceiveThread() {
        new Thread(() -> {
            String msgStr;
            try {
                while (isConnected && (msgStr = in.readLine()) != null) {
                    Message msg = Message.deserialize(msgStr);
                    if (msg == null) continue;

                    // 处理不同类型的消息
                    switch (msg.getType()) {
                        case Message.CHESS:
                            // 落子消息：解析坐标并更新本地棋盘
                            String[] pos = msg.getContent().split(",");
                            int row = Integer.parseInt(pos[0]);
                            int col = Integer.parseInt(pos[1]);
                            chessBoardPanel.getBoard().placeChess(row, col);
                            chessBoardPanel.repaint();
                            break;
                        case Message.CHAT:
                            // 聊天消息：显示在聊天区域（对方发送）
                            String sender = (msg.getSender() == Constant.BLACK) ? "黑棋" : "白棋";
                            chessBoardPanel.getMainFrame().getChatTextArea()
                                    .append(sender + "：" + msg.getContent() + "\n");
                            break;
                        case Message.TURN:
                            // 回合消息：更新当前玩家
                            int newPlayer = Integer.parseInt(msg.getContent());
                            chessBoardPanel.getBoard().setCurrentPlayer(newPlayer);
                            chessBoardPanel.repaint();
                            break;
                        case Message.WIN:
                            // 获胜消息：标记游戏结束 + 触发gameOver逻辑 + 显示提示
                            String winner = (msg.getSender() == Constant.BLACK) ? "黑棋" : "白棋";
                            // 标记游戏结束（禁止继续落子）
                            chessBoardPanel.isGameOver = true;
                            // 显示获胜日志
                            chessBoardPanel.getMainFrame().getChatTextArea()
                                    .append(winner + "获胜！\n");
                            int winnerColor = msg.getSender();
                            chessBoardPanel.gameOver(winnerColor);
                            break;
                        case Message.RESET:
                            // 重置消息：清空棋盘
                            chessBoardPanel.resetBoard();
                            chessBoardPanel.getMainFrame().getChatTextArea()
                                    .append("棋盘已重置！\n");
                            break;
                        case Message.RESET_REQUEST:
                            // 收到对方的重置请求，更新本地同意状态
                            int requester = msg.getSender();
                            // 通过chessBoardPanel获取Board实例
                            if (requester == Constant.BLACK) {
                                chessBoardPanel.getBoard().setBlackAgreeReset(true);
                            } else {
                                chessBoardPanel.getBoard().setWhiteAgreeReset(true);
                            }
                            // 同步同意状态给对方
                            sendMessage(new Message(Message.RESET_RESPONSE,
                                    chessBoardPanel.getBoard().isBlackAgreeReset() + "," + chessBoardPanel.getBoard().isWhiteAgreeReset(),
                                    chessBoardPanel.getMyColor()));
                            break;

                        case Message.RESET_RESPONSE:
                            // 收到对方的同意状态，更新本地
                            String[] agrees = msg.getContent().split(",");
                            chessBoardPanel.getBoard().setBlackAgreeReset(Boolean.parseBoolean(agrees[0]));
                            chessBoardPanel.getBoard().setWhiteAgreeReset(Boolean.parseBoolean(agrees[1]));

                            // 检查双方是否都同意
                            if (chessBoardPanel.getBoard().isBlackAgreeReset() && chessBoardPanel.getBoard().isWhiteAgreeReset()) {
                                chessBoardPanel.resetBoard(); // 双方同意则重置棋盘
                                chessBoardPanel.getMainFrame().getChatTextArea().append("双方已同意，游戏重新开始！\n");
                                // 通知服务端执行重置
                                sendMessage(new Message(Message.RESET, "", chessBoardPanel.getMyColor()));
                            } else {
                                // 显示当前同意状态
                                String status = (chessBoardPanel.getBoard().isBlackAgreeReset() ? "黑棋已同意，" : "黑棋未同意，") +
                                        (chessBoardPanel.getBoard().isWhiteAgreeReset() ? "白棋已同意" : "白棋未同意");
                                chessBoardPanel.getMainFrame().getChatTextArea().append("再来一局请求：" + status + "\n");
                            }
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                isConnected = false;
            }
        }).start();
    }

    /**
     * 发送消息给服务端
     * @param message 消息对象
     */
    public void sendMessage(Message message) {
        if (isConnected) {
            try {
                out.write(message.serialize() + "\n");
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        isConnected = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}