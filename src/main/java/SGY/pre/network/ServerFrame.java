package SGY.pre.network;

import SGY.pre.util.Constant;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 服务端主窗口：包含棋盘和日志显示
 */
public class ServerFrame extends JFrame {
    private ServerSocket serverSocket;
    private Socket blackSocket; // 黑棋客户端
    private Socket whiteSocket; // 白棋客户端
    private BufferedReader blackIn;
    private OutputStreamWriter blackOut;
    private BufferedReader whiteIn;
    private OutputStreamWriter whiteOut;
    private boolean isRunning = false;

    // 服务端棋盘面板
    private ServerChessBoardPanel chessBoardPanel;
    // 日志显示区域
    private JTextArea logTextArea;

    public ServerFrame() {
        // 窗口设置
        setTitle("五子棋服务端（实时显示棋子）");
        setSize(900, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        // 初始化界面
        initUI();

        // 启动服务端
        startServer();
    }

    /**
     * 初始化界面布局
     */
    private void initUI() {
        // 主面板：BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());
        add(mainPanel);

        // 1. 左侧：棋盘面板
        chessBoardPanel = new ServerChessBoardPanel();
        mainPanel.add(chessBoardPanel, BorderLayout.CENTER);

        // 2. 右侧：日志面板（宽度300）
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setPreferredSize(new Dimension(300, 0));
        mainPanel.add(logPanel, BorderLayout.EAST);

        // 日志标题
        JLabel logLabel = new JLabel("服务端日志");
        logLabel.setHorizontalAlignment(SwingConstants.CENTER);
        logPanel.add(logLabel, BorderLayout.NORTH);

        // 日志显示区域
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        logTextArea.setLineWrap(true);
        logPanel.add(new JScrollPane(logTextArea), BorderLayout.CENTER);

        // 重置棋盘按钮
        JButton resetBtn = new JButton("重置棋盘");
        resetBtn.addActionListener(e -> {
            // 重置服务端棋盘
            chessBoardPanel.resetBoard();
            // 发送重置消息给双方客户端
            sendResetMessage();
            logTextArea.append("服务端手动重置棋盘！\n");
        });
        logPanel.add(resetBtn, BorderLayout.SOUTH);
    }

    /**
     * 启动服务端
     */
    private void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(Constant.PORT);
                isRunning = true;
                log("服务端已启动，端口：" + Constant.PORT + "，等待玩家连接...");

                // 等待黑棋客户端连接
                blackSocket = serverSocket.accept();
                log("黑棋玩家已连接：" + blackSocket.getInetAddress());
                blackIn = new BufferedReader(new InputStreamReader(blackSocket.getInputStream()));
                blackOut = new OutputStreamWriter(blackSocket.getOutputStream());

                // 等待白棋客户端连接
                whiteSocket = serverSocket.accept();
                log("白棋玩家已连接：" + whiteSocket.getInetAddress());
                whiteIn = new BufferedReader(new InputStreamReader(whiteSocket.getInputStream()));
                whiteOut = new OutputStreamWriter(whiteSocket.getOutputStream());

                // 转发黑棋消息到白棋，并更新服务端棋盘
                new Thread(() -> {
                    String msgStr;
                    try {
                        while (isRunning && (msgStr = blackIn.readLine()) != null) {
                            log("收到黑棋消息：" + msgStr);
                            // 转发给白棋
                            whiteOut.write(msgStr + "\n");
                            whiteOut.flush();
                            // 解析落子消息，更新服务端棋盘
                            parseMessage(msgStr, Constant.BLACK);
                        }
                    } catch (IOException e) {
                        log("黑棋玩家断开连接：" + e.getMessage());
                    }
                }).start();

                // 转发白棋消息到黑棋，并更新服务端棋盘
                new Thread(() -> {
                    String msgStr;
                    try {
                        while (isRunning && (msgStr = whiteIn.readLine()) != null) {
                            log("收到白棋消息：" + msgStr);
                            // 转发给黑棋
                            blackOut.write(msgStr + "\n");
                            blackOut.flush();
                            // 解析落子消息，更新服务端棋盘
                            parseMessage(msgStr, Constant.WHITE);
                        }
                    } catch (IOException e) {
                        log("白棋玩家断开连接：" + e.getMessage());
                    }
                }).start();

            } catch (IOException e) {
                log("服务端启动失败：" + e.getMessage());
            }
        }).start();
    }

    /**
     * 解析消息，更新服务端棋盘并处理回合同步
     * @param msgStr 序列化后的消息
     * @param color 发送方颜色（黑/白）
     */
    private void parseMessage(String msgStr, int color) {
        Message message = Message.deserialize(msgStr);
        if (message == null) {
            return;
        }
        switch (message.getType()) {
            case Message.CHESS:
                // 落子消息：解析坐标
                String[] pos = message.getContent().split(",");
                int row = Integer.parseInt(pos[0]);
                int col = Integer.parseInt(pos[1]);
                // 服务端落子（使用发送方颜色）
                chessBoardPanel.placeChess(row, col, color);
                log("服务端更新棋盘：" + (color == Constant.BLACK ? "黑棋" : "白棋") + "(" + row + "," + col + ")");

                // 发送回合切换消息（通知双方下一个玩家）
                int nextPlayer = (color == Constant.BLACK) ? Constant.WHITE : Constant.BLACK;
                Message turnMsg = new Message(Message.TURN, String.valueOf(nextPlayer), 0);
                try {
                    if (blackOut != null) {
                        blackOut.write(turnMsg.serialize() + "\n");
                        blackOut.flush();
                    }
                    if (whiteOut != null) {
                        whiteOut.write(turnMsg.serialize() + "\n");
                        whiteOut.flush();
                    }
                } catch (IOException e) {
                    log("发送回合消息失败：" + e.getMessage());
                }
                break;
            case Message.RESET:
                // 重置消息：清空棋盘
                chessBoardPanel.resetBoard();
                log("服务端棋盘已重置");
                break;
            case Message.WIN:
                // 转发获胜消息给另一方
                log(message.getContent() + "获胜！");
                try {
                    if (color == Constant.BLACK) {
                        // 黑棋获胜，转发给白棋
                        if (whiteOut != null) {
                            whiteOut.write(msgStr + "\n");
                            whiteOut.flush();
                        }
                    } else {
                        // 白棋获胜，转发给黑棋
                        if (blackOut != null) {
                            blackOut.write(msgStr + "\n");
                            blackOut.flush();
                        }
                    }
                } catch (IOException e) {
                    log("转发获胜消息失败：" + e.getMessage());
                }
                break;
            case Message.CHAT:
                // 聊天消息记录日志（服务端可见）
                log((color == Constant.BLACK ? "黑棋" : "白棋") + "说：" + message.getContent());
                try {
                    if (color == Constant.BLACK && whiteOut != null) {
                        whiteOut.write(msgStr + "\n");
                        whiteOut.flush();
                    } else if (color == Constant.WHITE && blackOut != null) {
                        blackOut.write(msgStr + "\n");
                        blackOut.flush();
                    }
                } catch (IOException e) {
                    log("转发聊天消息失败：" + e.getMessage());
                }
                break;
        }
        // 处理重置相关消息的转发（修复变量引用错误，使用message而非msg）
        if (message.getType().equals(Message.RESET_REQUEST) || message.getType().equals(Message.RESET_RESPONSE)) {
            try {
                // 转发重置相关消息给另一方
                if (color == Constant.BLACK) {
                    whiteOut.write(msgStr + "\n");
                    whiteOut.flush();
                } else {
                    blackOut.write(msgStr + "\n");
                    blackOut.flush();
                }
            } catch (IOException e) {
                log("转发重置消息失败：" + e.getMessage());
            }
        }
    }

    /**
     * 发送重置消息给双方客户端
     */
    private void sendResetMessage() {
        try {
            if (blackOut != null) {
                blackOut.write(new Message(Message.RESET, "", 0).serialize() + "\n");
                blackOut.flush();
            }
            if (whiteOut != null) {
                whiteOut.write(new Message(Message.RESET, "", 0).serialize() + "\n");
                whiteOut.flush();
            }
        } catch (IOException e) {
            log("发送重置消息失败：" + e.getMessage());
        }
    }

    /**
     * 停止服务端
     */
    public void stopServer() {
        isRunning = false;
        try {
            if (blackIn != null) blackIn.close();
            if (blackOut != null) blackOut.close();
            if (blackSocket != null) blackSocket.close();
            if (whiteIn != null) whiteIn.close();
            if (whiteOut != null) whiteOut.close();
            if (whiteSocket != null) whiteSocket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            log("停止服务端失败：" + e.getMessage());
        }
        log("服务端已停止");
    }

    /**
     * 日志输出（线程安全）
     * @param msg 日志内容
     */
    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logTextArea.append(msg + "\n");
            // 自动滚动到最后一行
            logTextArea.setCaretPosition(logTextArea.getText().length());
        });
    }

    /**
     * 启动服务端窗口
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ServerFrame().setVisible(true);
        });
    }
}