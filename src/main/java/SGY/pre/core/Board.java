package SGY.pre.core;

import SGY.pre.util.Constant;

/**
 * 棋盘核心类，管理落子状态、当前玩家和胜负判断
 */
public class Board {
    private int[][] chessBoard; // 棋盘数据（0-空，1-黑，2-白）
    private int currentPlayer; // 当前玩家（1-黑，2-白）
    private ChessStack chessStack; // 落子记录栈（用于悔棋）
    // 新增：重置请求的同意状态
    private boolean blackAgreeReset; // 黑棋是否同意重置
    private boolean whiteAgreeReset; // 白棋是否同意重置

    /**
     * 初始化棋盘（15x15默认大小）
     */
    public Board() {
        chessBoard = new int[Constant.BOARD_ROWS][Constant.BOARD_COLS];
        currentPlayer = Constant.BLACK; // 黑棋先行
        chessStack = new ChessStack();
        // 初始化同意状态为false
        blackAgreeReset = false;
        whiteAgreeReset = false;
    }

    /**
     * 落子操作
     * @param row 行坐标
     * @param col 列坐标
     * @return 是否落子成功（位置有效且为空）
     */
    public boolean placeChess(int row, int col) {
        if (row < 0 || row >= Constant.BOARD_ROWS || col < 0 || col >= Constant.BOARD_COLS) {
            return false; // 超出棋盘范围
        }
        if (chessBoard[row][col] != Constant.EMPTY) {
            return false; // 位置已被占用
        }
        // 记录落子并更新棋盘
        chessBoard[row][col] = currentPlayer;
        chessStack.push(row + "," + col + "," + currentPlayer); // 保存落子信息（行,列,颜色）
        return true;
    }

    /**
     * 悔棋操作
     * @return 是否悔棋成功
     */
    public boolean regret() {
        String lastStep = chessStack.pop();
        if (lastStep == null) {
            return false; // 无记录可悔棋
        }
        String[] parts = lastStep.split(",");
        int row = Integer.parseInt(parts[0]);
        int col = Integer.parseInt(parts[1]);
        chessBoard[row][col] = Constant.EMPTY; // 清空位置
        switchPlayer(); // 悔棋后切换回上一玩家
        return true;
    }

    /**
     * 切换当前玩家（黑↔白）
     */
    public void switchPlayer() {
        currentPlayer = (currentPlayer == Constant.BLACK) ? Constant.WHITE : Constant.BLACK;
    }

    /**
     * 判断是否获胜（当前落子位置是否形成五子连珠）
     * @param row 最后落子的行
     * @param col 最后落子的列
     * @return 是否获胜
     */
    public boolean isWin(int row, int col) {
        int color = chessBoard[row][col];
        if (color == Constant.EMPTY) {
            return false;
        }

        // 检查方向：水平、垂直、左斜、右斜
        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        for (int[] dir : directions) {
            int count = 1; // 当前位置算1个
            // 正向统计
            for (int i = 1; i < 5; i++) {
                int r = row + i * dir[0];
                int c = col + i * dir[1];
                if (r < 0 || r >= Constant.BOARD_ROWS || c < 0 || c >= Constant.BOARD_COLS
                        || chessBoard[r][c] != color) {
                    break;
                }
                count++;
            }
            // 反向统计
            for (int i = 1; i < 5; i++) {
                int r = row - i * dir[0];
                int c = col - i * dir[1];
                if (r < 0 || r >= Constant.BOARD_ROWS || c < 0 || c >= Constant.BOARD_COLS
                        || chessBoard[r][c] != color) {
                    break;
                }
                count++;
            }
            if (count >= 5) {
                return true; // 五子连珠
            }
        }
        return false;
    }

    /**
     * 重置棋盘
     */
    public void resetBoard() {
        chessBoard = new int[Constant.BOARD_ROWS][Constant.BOARD_COLS];
        currentPlayer = Constant.BLACK;
        chessStack.clear();
        // 重置同意状态
        blackAgreeReset = false;
        whiteAgreeReset = false;
    }

    // 新增：重置同意状态的Getter和Setter方法
    public boolean isBlackAgreeReset() {
        return blackAgreeReset;
    }

    public void setBlackAgreeReset(boolean blackAgreeReset) {
        this.blackAgreeReset = blackAgreeReset;
    }

    public boolean isWhiteAgreeReset() {
        return whiteAgreeReset;
    }

    public void setWhiteAgreeReset(boolean whiteAgreeReset) {
        this.whiteAgreeReset = whiteAgreeReset;
    }

    // 原有Getter和Setter方法
    public int[][] getChessBoard() {
        return chessBoard;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(int player) {
        this.currentPlayer = player;
    }
}
