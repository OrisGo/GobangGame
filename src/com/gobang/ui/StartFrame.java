package com.gobang.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 五子棋启动主界面（主界面）：新增模式选择
 */
public class StartFrame extends JFrame {
    // 新增：模式常量（用于传递给MainFrame）
    public static final int MODE_AI = 1; // 人机对战
    public static final int MODE_LOCAL_TWO_PLAYER = 2; // 本地双人对战
    public static final int MODE_ONLINE = 3; // 联机对战

    // 窗口计数（用于区分多窗口）
    private static int windowCount = 0;

    public StartFrame() {
        windowCount++;
        // 窗口设置
        setTitle("五子棋 - 主界面（窗口" + windowCount + "）");
        setSize(500, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        // 初始化界面布局
        initUI();
    }

    /**
     * 初始化界面元素（重构：模式选择按钮）
     */
    private void initUI() {
        // 主面板：垂直BoxLayout，居中对齐
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(new Color(245, 245, 245));
        add(mainPanel);

        // 1. 标题标签
        JLabel titleLabel = new JLabel("五子棋对战游戏");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 36));
        titleLabel.setForeground(new Color(0, 102, 204));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(50, 0, 30, 0));
        mainPanel.add(titleLabel);

        // 2. 模式选择按钮面板（垂直排列）
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(buttonPanel);

        // 按钮样式设置
        Dimension buttonSize = new Dimension(200, 50);
        Font buttonFont = new Font("微软雅黑", Font.PLAIN, 18);

        // （1）人机对战按钮
        JButton aiGameBtn = new JButton("人机对战");
        styleButton(aiGameBtn, buttonSize, buttonFont);
        aiGameBtn.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        buttonPanel.add(aiGameBtn);

        // （2）本地双人对战按钮（人人对战-本地）
        JButton localTwoPlayerBtn = new JButton("本地双人对战");
        styleButton(localTwoPlayerBtn, buttonSize, buttonFont);
        localTwoPlayerBtn.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        buttonPanel.add(localTwoPlayerBtn);

        // （3）联机对战按钮（人人对战-联机）
        JButton onlineGameBtn = new JButton("联机对战");
        styleButton(onlineGameBtn, buttonSize, buttonFont);
        onlineGameBtn.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        buttonPanel.add(onlineGameBtn);

        // （4）关于按钮（保留）
        JButton aboutBtn = new JButton("关于游戏");
        styleButton(aboutBtn, buttonSize, buttonFont);
        aboutBtn.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        buttonPanel.add(aboutBtn);

        // （5）退出按钮（保留）
        JButton exitBtn = new JButton("退出游戏");
        styleButton(exitBtn, buttonSize, buttonFont);
        exitBtn.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        buttonPanel.add(exitBtn);

        // 3. 按钮事件处理
        // （1）人机对战：打开MainFrame并传入AI模式
        aiGameBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 打开MainFrame，传入模式参数：人机对战
                new MainFrame(MODE_AI).setVisible(true);
                StartFrame.this.setVisible(false); // 隐藏主界面
            }
        });

        // （2）本地双人对战：打开MainFrame并传入本地双人模式
        localTwoPlayerBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new MainFrame(MODE_LOCAL_TWO_PLAYER).setVisible(true);
                StartFrame.this.setVisible(false);
            }
        });

        // （3）联机对战：先弹出选择（服务端/客户端），再打开MainFrame
        onlineGameBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 弹出选择框：服务端/客户端
                String[] options = {"作为服务端", "作为客户端"};
                int choice = JOptionPane.showOptionDialog(
                        StartFrame.this,
                        "请选择联机角色：",
                        "联机对战",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]
                );
                if (choice == 0) {
                    // 作为服务端：打开MainFrame并传入联机模式（服务端）
                    new MainFrame(MODE_ONLINE, true).setVisible(false);
                } else if (choice == 1) {
                    // 作为客户端：打开MainFrame并传入联机模式（客户端）
                    new MainFrame(MODE_ONLINE, false).setVisible(true);
                }
                StartFrame.this.setVisible(false);
            }
        });

        // （4）关于游戏（原有逻辑不变）
        aboutBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(
                        StartFrame.this,
                        "五子棋对战游戏\n\n功能说明：\n1. 人机对抗（支持悔棋）\n2. 本地双人对战（支持悔棋）\n3. 联机对战（支持聊天、悔棋）\n\n版本：v1.0\n作者：五子棋开发团队",
                        "关于游戏",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });

        // （5）退出游戏（原有逻辑不变）
        exitBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
    }

    /**
     * 统一设置按钮样式（原有方法不变）
     */
    private void styleButton(JButton button, Dimension size, Font font) {
        button.setPreferredSize(size);
        button.setMaximumSize(size);
        button.setFont(font);
        button.setBackground(new Color(0, 102, 204));
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        // 鼠标悬停效果
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(0, 128, 255));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(0, 102, 204));
            }
        });
    }

    /**
     * 程序入口（原有逻辑不变）
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new StartFrame().setVisible(true);
            }
        });
    }
}