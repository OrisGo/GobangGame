package com.gobang.ui;

import com.gobang.ai.AI;
import com.gobang.core.Board;
import com.gobang.core.ChessStack;
import com.gobang.network.Client;
import com.gobang.network.Message;
import com.gobang.util.Constant;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 五子棋棋盘面板（核心UI组件，处理落子、绘制、模式逻辑）
 */
public class ChessBoardPanel extends JPanel {
    // 游戏核心逻辑
    private Board board;
    private ChessStack chessStack;
    // 模式相关
    private boolean isAI; // 人机模式
    private boolean isLocalTwoPlayer; // 本地双人模式
    private boolean isOnline; // 联机模式
    private AI ai; // AI实例
    private Client client; // 客户端网络实例
    private int myColor; // 联机模式下自己的棋子颜色（BLACK/WHITE）
    private int playerColor; // 人机模式玩家颜色
    private int aiColor; // 人机模式AI颜色
    // 持有MainFrame引用，用于访问聊天区域
    private MainFrame mainFrame;
    // 游戏状态
    public boolean isGameOver = false;

    /**
     * 构造方法（供非联机模式使用，默认无MainFrame）
     */
    public ChessBoardPanel() {
        this(null);
    }

    /**
     * 主构造方法（传入MainFrame引用，用于访问聊天区域）
     * @param mainFrame 主窗口实例
     */
    public ChessBoardPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        init();
    }

    /**
     * 初始化棋盘面板
     */
    private void init() {
        this.board = new Board();
        this.chessStack = new ChessStack();
        // 设置面板大小（基于棋盘格子数和格子尺寸）
        setPreferredSize(new Dimension(
                Constant.GRID_SIZE * (Constant.BOARD_COLS - 1) + Constant.GRID_SIZE,
                Constant.GRID_SIZE * (Constant.BOARD_ROWS - 1) + Constant.GRID_SIZE
        ));
        // 注册鼠标点击事件（落子）
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e);
            }
        });
    }

    /**
     * 处理鼠标点击落子逻辑
     */
    private void handleMouseClick(MouseEvent e) {
        if (isGameOver) {
            JOptionPane.showMessageDialog(this, "游戏已结束，请重置棋盘！");
            return;
        }

        // 计算最近的棋盘节点坐标
        int col = Math.round((float)(e.getX() - Constant.GRID_SIZE / 2) / Constant.GRID_SIZE);
        int row = Math.round((float)(e.getY() - Constant.GRID_SIZE / 2) / Constant.GRID_SIZE);

        // 计算与实际节点的距离，判断是否在有效范围内（节点附近GRID_SIZE/3范围内）
        int actualX = col * Constant.GRID_SIZE;
        int actualY = row * Constant.GRID_SIZE;
        int distanceX = Math.abs(e.getX() - actualX);
        int distanceY = Math.abs(e.getY() - actualY);

        // 如果距离太远，不视为有效点击
        if (distanceX > Constant.GRID_SIZE / 3 || distanceY > Constant.GRID_SIZE / 3) {
            return;
        }

        // 校验坐标合法性
        if (row < 0 || row >= Constant.BOARD_ROWS || col < 0 || col >= Constant.BOARD_COLS) {
            return;
        }

        // 校验当前位置是否已有棋子
        if (board.getChessBoard()[row][col] != Constant.EMPTY) {
            JOptionPane.showMessageDialog(this, "该位置已有棋子，请重新选择！");
            return;
        }

        // 根据不同模式处理落子
        if (isOnline) {
            handleOnlineChess(row, col);
        } else if (isAI) {
            handleAIChess(row, col);
        } else if (isLocalTwoPlayer) {
            handleLocalTwoPlayerChess(row, col);
        } else {
            // 默认本地单人模式（等同于本地双人，仅玩家操作）
            handleLocalTwoPlayerChess(row, col);
        }
    }

    /**
     * 联机模式落子处理
     */
    private void handleOnlineChess(int row, int col) {
        // 校验是否是自己的回合（只有自己的颜色回合才能落子）
        if (board.getCurrentPlayer() != myColor) {
            JOptionPane.showMessageDialog(this, "还未到你的回合，请等待！");
            return;
        }
        // 执行落子（仅记录，不本地切换回合，由服务端同步）
        if (board.getChessBoard()[row][col] == Constant.EMPTY) {
            // 本地临时落子（服务端会同步正确状态）
            board.getChessBoard()[row][col] = myColor;
            chessStack.push(row + "," + col + "," + myColor);
            repaint();

            // 发送落子消息到服务端
            Message chessMsg = new Message(Message.CHESS, row + "," + col, myColor);
            client.sendMessage(chessMsg);

            // 检查是否获胜（本地预判）
            if (board.isWin(row, col)) {
                isGameOver = true;
                Message winMsg = new Message(Message.WIN, (myColor == Constant.BLACK ? "黑棋" : "白棋"), myColor);
                client.sendMessage(winMsg);
                // 触发游戏结束逻辑
                gameOver(myColor);
                return;
            }
        }
    }

    /**
     * 人机模式落子处理
     */
    private void handleAIChess(int row, int col) {
        // 确保当前是玩家回合
        if (board.getCurrentPlayer() != playerColor) {
            JOptionPane.showMessageDialog(this, "请等待AI落子...");
            return;
        }

        // 玩家落子
        if (board.placeChess(row, col)) {
            chessStack.push(row + "," + col + "," + playerColor);
            repaint();

            // 检查玩家是否获胜
            if (board.isWin(row, col)) {
                isGameOver = true;
                gameOver(playerColor);
                return;
            }

            // 切换到AI回合
            board.setCurrentPlayer(aiColor);
            repaint();

            // AI落子（延迟一小段时间，提升体验）
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                SwingUtilities.invokeLater(() -> {
                    if (isGameOver) return;

                    // AI落子
                    int[] aiPos = ai.calculateAIPos();
                    int aiRow = aiPos[0];
                    int aiCol = aiPos[1];
                    board.placeChess(aiRow, aiCol);
                    chessStack.push(aiRow + "," + aiCol + "," + aiColor);
                    repaint();

                    // 检查AI是否获胜
                    if (board.isWin(aiRow, aiCol)) {
                        isGameOver = true;
                        gameOver(aiColor);
                        return;
                    }

                    // 切换回玩家回合
                    board.setCurrentPlayer(playerColor);
                    repaint();
                });
            }).start();
        }
    }

    /**
     * 本地双人模式落子处理
     */
    private void handleLocalTwoPlayerChess(int row, int col) {
        // 执行落子
        int currentPlayer = board.getCurrentPlayer();
        if (board.placeChess(row, col)) {
            chessStack.push(row + "," + col + "," + currentPlayer);
            repaint();

            // 检查是否获胜
            if (board.isWin(row, col)) {
                isGameOver = true;
                gameOver(currentPlayer);
                return;
            }

            // 切换玩家
            board.switchPlayer();
        }
    }

    /**
     * 绘制棋盘（重写JPanel的paintComponent方法）
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        // 抗锯齿（让线条和棋子更平滑）
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. 绘制棋盘背景（浅木色）
        g2d.setColor(new Color(240, 220, 180));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // 2. 绘制棋盘网格线
        g2d.setColor(Color.BLACK);
        // 绘制竖线
        for (int col = 0; col < Constant.BOARD_COLS; col++) {
            int x = col * Constant.GRID_SIZE;
            g2d.drawLine(x, 0, x, Constant.GRID_SIZE * (Constant.BOARD_ROWS - 1));
        }
        // 绘制横线
        for (int row = 0; row < Constant.BOARD_ROWS; row++) {
            int y = row * Constant.GRID_SIZE;
            g2d.drawLine(0, y, Constant.GRID_SIZE * (Constant.BOARD_COLS - 1), y);
        }

        // 3. 绘制棋子
        int[][] chessBoard = board.getChessBoard();
        for (int row = 0; row < Constant.BOARD_ROWS; row++) {
            for (int col = 0; col < Constant.BOARD_COLS; col++) {
                int x = col * Constant.GRID_SIZE;
                int y = row * Constant.GRID_SIZE;
                // 黑棋
                if (chessBoard[row][col] == Constant.BLACK) {
                    g2d.setColor(Color.BLACK);
                    g2d.fillOval(x - Constant.CHESS_SIZE / 2, y - Constant.CHESS_SIZE / 2, Constant.CHESS_SIZE, Constant.CHESS_SIZE);
                    // 绘制棋子边框（增强视觉效果）
                    g2d.setColor(Color.DARK_GRAY);
                    g2d.drawOval(x - Constant.CHESS_SIZE / 2, y - Constant.CHESS_SIZE / 2, Constant.CHESS_SIZE, Constant.CHESS_SIZE);
                }
                // 白棋
                else if (chessBoard[row][col] == Constant.WHITE) {
                    g2d.setColor(Color.WHITE);
                    g2d.fillOval(x - Constant.CHESS_SIZE / 2, y - Constant.CHESS_SIZE / 2, Constant.CHESS_SIZE, Constant.CHESS_SIZE);
                    // 绘制棋子边框（增强视觉效果）
                    g2d.setColor(Color.LIGHT_GRAY);
                    g2d.drawOval(x - Constant.CHESS_SIZE / 2, y - Constant.CHESS_SIZE / 2, Constant.CHESS_SIZE, Constant.CHESS_SIZE);
                }
            }
        }

        // 4. 绘制当前玩家提示（右上角）
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("宋体", Font.BOLD, 16));
        String tip = "当前玩家：" + (board.getCurrentPlayer() == Constant.BLACK ? "黑棋" : "白棋");
        g2d.drawString(tip, Constant.GRID_SIZE * (Constant.BOARD_COLS - 5), Constant.GRID_SIZE * 2);
    }

    /**
     * 重置棋盘
     */
    public void resetBoard() {
        board.resetBoard();
        chessStack.clear();
        isGameOver = false;
        repaint();
        // 如果有MainFrame且聊天区域存在，添加重置日志
        if (mainFrame != null && mainFrame.getChatTextArea() != null) {
            mainFrame.getChatTextArea().append("棋盘已重置！\n");
        }
        // 人机模式如果AI是黑棋，AI先落子
        if (isAI && aiColor == Constant.BLACK) {
            SwingUtilities.invokeLater(this::makeAIMove);
        }
    }

    /**
     * 悔棋操作
     */
    public void regretChess() {
        if (isOnline) {
            JOptionPane.showMessageDialog(this, "联机模式暂不支持悔棋！");
            return;
        }
        if (chessStack.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有可悔的棋步！");
            return;
        }
        // 人机模式悔棋需要撤销两步（玩家+AI）
        if (isAI) {
            if (chessStack.size() < 2) {
                JOptionPane.showMessageDialog(this, "仅能悔棋完整回合！");
                return;
            }
            chessStack.pop(); // 撤销AI步
            board.regret();
            chessStack.pop(); // 撤销玩家步
            board.regret();
        } else {
            // 本地双人模式撤销一步
            chessStack.pop();
            board.regret();
        }
        repaint();
        // 刷新聊天区域日志
        if (mainFrame != null && mainFrame.getChatTextArea() != null) {
            mainFrame.getChatTextArea().append("已悔棋！\n");
        }
    }

    /**
     * AI落子操作（供重置后自动落子调用）
     */
    public void makeAIMove() {
        if (isGameOver || !isAI || board.getCurrentPlayer() != aiColor) {
            return;
        }
        int[] aiPos = ai.calculateAIPos();
        int aiRow = aiPos[0];
        int aiCol = aiPos[1];
        board.placeChess(aiRow, aiCol);
        chessStack.push(aiRow + "," + aiCol + "," + aiColor);
        repaint();

        // 检查AI是否获胜
        if (board.isWin(aiRow,aiCol)) {
            isGameOver = true;
            gameOver(aiColor);
            return;
        }

        // 切换回玩家回合
        board.setCurrentPlayer(playerColor);
        repaint();
    }

    /**
     * 游戏结束处理
     * @param winnerColor 获胜方颜色
     */
    public void gameOver(int winnerColor) {
        String message;
        if (winnerColor == Constant.EMPTY) {
            message = "游戏结束，平局！";
        } else {
            if (isAI) {
                message = (winnerColor == playerColor) ? "游戏结束，你获胜了！" : "游戏结束，AI获胜了！";
            } else if (isLocalTwoPlayer) {
                message = "游戏结束，" + (winnerColor == Constant.BLACK ? "黑棋" : "白棋") + "获胜！";
            } else { // 联机模式
                message = "游戏结束，" + (winnerColor == myColor ? "你获胜了！" : "对手获胜了！");
            }
        }

        // 显示选择对话框
        Object[] options = {"再来一局", "返回主界面"};
        int choice = JOptionPane.showOptionDialog(
                this,
                message + "\n请选择下一步操作：",
                "游戏结束",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 0) {
            // 再来一局
            resetBoard();
            // 联机模式需要通知对手
            if (isOnline && client != null) {
                client.sendMessage(new Message(Message.RESET, "", myColor));
            }
        } else {
            // 返回主界面
            Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
            parentFrame.dispose();
            new StartFrame().setVisible(true);
        }
    }

    /**
     * 设置人机模式（支持颜色选择）
     */
    public void setAIMode(AI ai, int playerColor, int aiColor) {
        this.isAI = true;
        this.isLocalTwoPlayer = false;
        this.isOnline = false;
        this.ai = ai;
        this.playerColor = playerColor;
        this.aiColor = aiColor;
        this.board.setCurrentPlayer(Constant.BLACK); // 黑棋先行
    }

    /**
     * 设置本地双人模式
     */
    public void setLocalTwoPlayerMode() {
        this.isLocalTwoPlayer = true;
        this.isAI = false;
        this.isOnline = false;
        this.board.setCurrentPlayer(Constant.BLACK); // 黑棋先行
    }

    /**
     * 设置联机模式
     */
    public void setOnlineMode(Client client, int myColor) {
        this.isOnline = true;
        this.isAI = false;
        this.isLocalTwoPlayer = false;
        this.client = client;
        this.myColor = myColor;
        this.board.setCurrentPlayer(Constant.BLACK); // 黑棋先行
    }

    // Getters and Setters
    public Board getBoard() {
        return board;
    }

    public Client getClient() {
        return client;
    }

    public int getMyColor() {
        return myColor;
    }

    public MainFrame getMainFrame() {
        return mainFrame;
    }
}