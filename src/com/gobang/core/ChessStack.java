package com.gobang.core;

import java.util.Stack;

/**
 * 落子记录栈，用于实现悔棋功能
 */
public class ChessStack {
    private Stack<String> stack; // 存储格式："行,列,颜色"

    public ChessStack() {
        stack = new Stack<>();
    }

    /**
     * 入栈（记录落子）
     * @param step 落子信息字符串
     */
    public void push(String step) {
        stack.push(step);
    }

    /**
     * 出栈（悔棋：移除最后一步）
     * @return 最后一步的落子信息
     */
    public String pop() {
        if (!stack.isEmpty()) {
            return stack.pop();
        }
        return null;
    }

    /**
     * 清空栈（重置棋盘时使用）
     */
    public void clear() {
        stack.clear();
    }

    /**
     * 判断栈是否为空
     * @return 是否为空
     */
    public boolean isEmpty() {
        return stack.isEmpty();
    }
    public int size() {
        return stack.size();
    }
}
