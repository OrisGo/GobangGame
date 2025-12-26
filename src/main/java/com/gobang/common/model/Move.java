package com.gobang.common.model;

import java.io.Serializable;

/**
 * 封装一次落子的坐标和颜色信息
 */
public record Move(int row, int col, Piece color) implements Serializable {

    @Override
    public String toString() {
        return String.format("%s落子于(%d, %d)", color.getName(), row, col);
    }
}