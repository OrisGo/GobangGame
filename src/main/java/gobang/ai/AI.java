package gobang.ai;

import gobang.core.Board;
import gobang.util.Constant;

/**
 * 五子棋AI类，实现贪心策略落子逻辑（包含攻防）
 */
public class AI {
    private Board board; // 棋盘实例
    private int aiColor; // AI棋子颜色
    private int playerColor; // 玩家棋子颜色

    /**
     * 构造方法：初始化AI与棋盘的关联及颜色
     * @param board 棋盘对象
     * @param aiColor AI的颜色
     * @param playerColor 玩家的颜色
     */
    public AI(Board board, int aiColor, int playerColor) {
        this.board = board;
        this.aiColor = aiColor;
        this.playerColor = playerColor;
    }

    /**
     * 计算AI的落子位置（核心逻辑）
     * 策略：优先攻防，再选中心及扩展位置
     * @return int[2]：索引0为行，索引1为列（落子坐标）
     */
    public int[] calculateAIPos() {
        // 1. 检查是否有能直接获胜的位置（进攻）
        int[] winPos = findWinningPosition(aiColor);
        if (winPos != null) {
            return winPos;
        }

        // 2. 检查是否需要防守（阻止玩家获胜）
        int[] defendPos = findWinningPosition(playerColor);
        if (defendPos != null) {
            return defendPos;
        }

        // 3. 优先选择棋盘中心位置（15x15棋盘中心为7,7）
        int centerRow = Constant.BOARD_ROWS / 2;
        int centerCol = Constant.BOARD_COLS / 2;
        if (board.getChessBoard()[centerRow][centerCol] == Constant.EMPTY) {
            return new int[]{centerRow, centerCol};
        }

        // 4. 从中心向外逐层扩展（十字和对角线方向）
        for (int step = 1; step < Math.max(Constant.BOARD_ROWS, Constant.BOARD_COLS) / 2; step++) {
            // 上
            if (isValidEmptyPos(centerRow - step, centerCol)) {
                return new int[]{centerRow - step, centerCol};
            }
            // 下
            if (isValidEmptyPos(centerRow + step, centerCol)) {
                return new int[]{centerRow + step, centerCol};
            }
            // 左
            if (isValidEmptyPos(centerRow, centerCol - step)) {
                return new int[]{centerRow, centerCol - step};
            }
            // 右
            if (isValidEmptyPos(centerRow, centerCol + step)) {
                return new int[]{centerRow, centerCol + step};
            }
            // 对角线（左上）
            if (isValidEmptyPos(centerRow - step, centerCol - step)) {
                return new int[]{centerRow - step, centerCol - step};
            }
            // 对角线（右下）
            if (isValidEmptyPos(centerRow + step, centerCol + step)) {
                return new int[]{centerRow + step, centerCol + step};
            }
            // 对角线（右上）
            if (isValidEmptyPos(centerRow - step, centerCol + step)) {
                return new int[]{centerRow - step, centerCol + step};
            }
            // 对角线（左下）
            if (isValidEmptyPos(centerRow + step, centerCol - step)) {
                return new int[]{centerRow + step, centerCol - step};
            }
        }

        // 5. 若上述位置均被占用，遍历整个棋盘找第一个空白位置
        for (int row = 0; row < Constant.BOARD_ROWS; row++) {
            for (int col = 0; col < Constant.BOARD_COLS; col++) {
                if (board.getChessBoard()[row][col] == Constant.EMPTY) {
                    return new int[]{row, col};
                }
            }
        }

        // 棋盘已满（理论上不会触发）
        return new int[]{-1, -1};
    }

    /**
     * 查找能形成五子连珠的获胜位置
     * @param color 要检查的棋子颜色
     * @return 获胜位置坐标，无则返回null
     */
    private int[] findWinningPosition(int color) {
        int[][] boardArr = board.getChessBoard();

        for (int row = 0; row < Constant.BOARD_ROWS; row++) {
            for (int col = 0; col < Constant.BOARD_COLS; col++) {
                if (boardArr[row][col] == Constant.EMPTY) {
                    // 模拟落子
                    boardArr[row][col] = color;
                    // 检查是否获胜
                    boolean isWin = board.isWin(row, col);
                    // 撤销模拟落子
                    boardArr[row][col] = Constant.EMPTY;

                    if (isWin) {
                        return new int[]{row, col};
                    }
                }
            }
        }
        return null;
    }

    /**
     * 辅助方法：判断位置是否有效且为空
     * @param row 行坐标
     * @param col 列坐标
     * @return 是否有效空白位置
     */
    private boolean isValidEmptyPos(int row, int col) {
        return row >= 0 && row < Constant.BOARD_ROWS
                && col >= 0 && col < Constant.BOARD_COLS
                && board.getChessBoard()[row][col] == Constant.EMPTY;
    }
}