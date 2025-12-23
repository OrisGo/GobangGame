package gobang.util;

import java.util.Stack;

/**
 * 棋子栈（存储落子坐标，用于悔棋）
 */
public class ChessStack {
    // 栈元素：格式为"行,列,颜色"（如"5,8,1"表示第5行第8列黑棋）
    private final Stack<String> stack = new Stack<>();

    // 入栈
    public void push(int row, int col, int color) {
        stack.push(row + "," + col + "," + color);
    }

    // 出栈（悔棋：移除最后一步）
    public String pop() {
        if (!stack.isEmpty()) {
            return stack.pop();
        }
        return null;
    }

    // 判断栈是否为空
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    // 获取栈大小
    public int size() {
        return stack.size();
    }

    // 清空栈
    public void clear() {
        stack.clear();
    }
}
