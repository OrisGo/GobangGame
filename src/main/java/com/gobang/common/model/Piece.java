package com.gobang.common.model;

/**
 * [Enum] 棋子类型/颜色
 */
public enum Piece {
    EMPTY(0, "无"),
    BLACK(1, "黑棋"),
    WHITE(2, "白棋");

    private final int value;
    private final String name;

    Piece(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() { return value; }
    public String getName() { return name; }

    /**
     * 获取对手的颜色
     */
    public Piece getOpposite() {
        if (this == BLACK) return WHITE;
        if (this == WHITE) return BLACK;
        return EMPTY;
    }
}