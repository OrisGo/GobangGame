package SGY.pre.ui;

import SGY.pre.ai.AI;
import SGY.pre.network.Client;
import SGY.pre.network.Message;
import SGY.pre.network.ServerFrame;
import SGY.pre.util.Constant;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MainFrame extends JFrame {
    private ChessBoardPanel chessBoardPanel;
    private JTextArea chatTextArea;
    private int gameMode; // 游戏模式：MODE_AI/MODE_LOCAL_TWO_PLAYER/MODE_ONLINE
    private boolean isServer; // 联机模式下：是否为服务端（仅MODE_ONLINE时有效）

    // 构造方法1：无参（兼容原有逻辑）
    public MainFrame() {
        this(StartFrame.MODE_AI); // 默认人机对战
    }

    // 构造方法2：接收游戏模式（人机/本地双人）
    public MainFrame(int gameMode) {
        this.gameMode = gameMode;
        this.isServer = false; // 非联机模式，该参数无效
        initFrame();
    }

    // 构造方法3：接收联机模式（服务端/客户端）
    public MainFrame(int gameMode, boolean isServer) {
        this.gameMode = gameMode;
        this.isServer = isServer;
        initFrame();
    }

    /**
     * 初始化窗口和模式
     */
    private void initFrame() {
        // 窗口设置
        setTitle("五子棋 - " + getModeName());
        setSize(800, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setResizable(false);

        // 初始化棋盘面板
        chessBoardPanel = new ChessBoardPanel(this); // 传入当前MainFrame
        // 初始化聊天区域（仅联机模式显示）
        chatTextArea = new JTextArea();
        chatTextArea.setEditable(false);
        chatTextArea.setLineWrap(true);

        // 根据模式初始化逻辑
        initModeLogic();

        // 初始化界面布局
        initLayout();
        // 初始化菜单
        initMenu();

        // 窗口关闭事件：返回主界面
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                new StartFrame().setVisible(true);
                dispose();
            }
        });
    }

    /**
     * 根据模式获取名称（用于窗口标题）
     */
    private String getModeName() {
        switch (gameMode) {
            case StartFrame.MODE_AI:
                return "人机对战";
            case StartFrame.MODE_LOCAL_TWO_PLAYER:
                return "本地双人对战";
            case StartFrame.MODE_ONLINE:
                return isServer ? "联机对战（服务端）" : "联机对战（客户端）";
            default:
                return "未知模式";
        }
    }

    /**
     * 根据模式初始化业务逻辑
     */
    private void initModeLogic() {
        switch (gameMode) {
            case StartFrame.MODE_AI:
                // 人机对战：让玩家选择棋子颜色
                String[] roles = {"黑棋", "白棋"};
                String role = (String) JOptionPane.showInputDialog(
                        this,
                        "选择你的棋子颜色：",
                        "角色选择",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        roles,
                        roles[0]
                );
                if (role == null) {
                    // 用户取消选择，返回主界面
                    new StartFrame().setVisible(true);
                    this.dispose();
                    return;
                }

                int playerColor = role.equals("黑棋") ? Constant.BLACK : Constant.WHITE;
                int aiColor = playerColor == Constant.BLACK ? Constant.WHITE : Constant.BLACK;

                // 初始化AI
                AI ai = new AI(chessBoardPanel.getBoard(), aiColor, playerColor);
                chessBoardPanel.setAIMode(ai, playerColor, aiColor);

                JOptionPane.showMessageDialog(this, "人机对战模式已启动！\n你是" + role + "，" +
                        (playerColor == Constant.BLACK ? "先行落子" : "AI先行落子"));
                break;

            case StartFrame.MODE_LOCAL_TWO_PLAYER:
                // 本地双人对战：启用本地双人模式
                chessBoardPanel.setLocalTwoPlayerMode();
                JOptionPane.showMessageDialog(this, "本地双人对战模式已启动！\n黑棋先下，白棋后下，轮流操作。");
                break;

            case StartFrame.MODE_ONLINE:
                // 联机对战：区分服务端/客户端
                if (isServer) {
                    // 启动服务端窗口
                    SwingUtilities.invokeLater(() -> {
                        new ServerFrame().setVisible(true);
                    });
                    JOptionPane.showMessageDialog(this, "服务端已启动！\n请启动客户端连接127.0.0.1");
                } else {
                    // 客户端：输入IP并连接
                    String ip = JOptionPane.showInputDialog(this, "请输入服务端IP地址：", Constant.LOCAL_HOST);
                    if (ip == null || ip.trim().isEmpty()) {
                        return;
                    }
                    // 选择角色（黑棋/白棋）
                    String[] onlineRoles = {"黑棋", "白棋"};
                    String onlineRole = (String) JOptionPane.showInputDialog(
                            this,
                            "选择你的棋子颜色：",
                            "角色选择",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            onlineRoles,
                            onlineRoles[0]
                    );
                    if (onlineRole == null) {
                        return;
                    }
                    // 连接服务端
                    Client client = new Client(chessBoardPanel); // 传入棋盘面板
                    if (client.connect(ip)) {
                        int myColor = onlineRole.equals("黑棋") ? Constant.BLACK : Constant.WHITE;
                        chessBoardPanel.setOnlineMode(client, myColor);
                        chatTextArea.append("已连接到服务端，你是" + onlineRole + "！\n");
                        // 提示回合信息
                        if (myColor == Constant.BLACK) {
                            chatTextArea.append("你是黑棋，先行落子！\n");
                        } else {
                            chatTextArea.append("你是白棋，请等待黑棋落子...\n");
                        }
                        JOptionPane.showMessageDialog(this, "联机对战（客户端）已启动！\n你是" + onlineRole + "，" + (myColor == Constant.BLACK ? "先行落子" : "等待黑棋落子") + "。");
                    } else {
                        JOptionPane.showMessageDialog(this, "连接服务端失败！");
                        // 返回主界面
                        new StartFrame().setVisible(true);
                        this.dispose();
                    }
                }
                break;
        }
    }

    public JTextArea getChatTextArea() {
        return chatTextArea;
    }

    /**
     * 初始化界面布局（根据模式显示不同布局）
     */
    private void initLayout() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        add(mainPanel);

        // 棋盘面板放在中间
        mainPanel.add(chessBoardPanel, BorderLayout.CENTER);

        // 仅联机模式显示右侧聊天区域
        if (gameMode == StartFrame.MODE_ONLINE) {
            JPanel chatPanel = new JPanel(new BorderLayout());
            chatPanel.setPreferredSize(new Dimension(200, 0));
            chatPanel.add(new JScrollPane(chatTextArea), BorderLayout.CENTER);

            // 聊天输入框
            JTextField chatInput = new JTextField();
            chatInput.addActionListener(e -> {
                String content = chatInput.getText().trim();
                if (!content.isEmpty() && chessBoardPanel.getClient() != null) {
                    // 发送聊天消息
                    Message chatMsg = new Message(
                            Message.CHAT,
                            content,
                            chessBoardPanel.getMyColor()
                    );
                    chessBoardPanel.getClient().sendMessage(chatMsg);
                    // 本地显示自己发送的消息
                    chatTextArea.append("我：" + content + "\n");
                    chatInput.setText(""); // 清空输入框
                }
            });

            chatPanel.add(chatInput, BorderLayout.SOUTH);
            mainPanel.add(chatPanel, BorderLayout.EAST);
        }
    }

    /**
     * 初始化菜单（仅保留悔棋、重置、退出）
     */
    private void initMenu() {
        JMenuBar menuBar = new JMenuBar();

        // 游戏菜单
        JMenu gameMenu = new JMenu("游戏");
        // 悔棋
        JMenuItem regretItem = new JMenuItem("悔棋");
        // 重置棋盘
        JMenuItem resetItem = new JMenuItem("重置棋盘");
        // 退出
        JMenuItem exitItem = new JMenuItem("退出");

        gameMenu.add(regretItem);
        gameMenu.add(resetItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);
        menuBar.add(gameMenu);
        setJMenuBar(menuBar);

        // 菜单事件处理
        regretItem.addActionListener(e -> {
            chessBoardPanel.regretChess();
            if (gameMode == StartFrame.MODE_ONLINE) {
                chatTextArea.append("已悔棋！\n");
            }
        });

        resetItem.addActionListener(e -> {
            chessBoardPanel.resetBoard();
            if (gameMode == StartFrame.MODE_ONLINE && chessBoardPanel.getClient() != null) {
                chessBoardPanel.getClient().sendMessage(new Message(Message.RESET, "", chessBoardPanel.getMyColor()));
                chatTextArea.append("棋盘已重置！\n");
            }
        });

        exitItem.addActionListener(e -> {
            System.exit(0);
        });
    }
}