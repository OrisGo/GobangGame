package com.gobang.client.player;

import com.gobang.common.logic.Game;
import com.gobang.common.model.Piece;

public class LocalPlayer implements Player {
    private Piece color;
    private String name;

    public LocalPlayer(String name, Piece color) {
        this.name = name;
        this.color = color;
    }

    @Override public Piece getColor() { return color; }
    @Override public void setColor(Piece color) { this.color = color; }
    @Override public String getName() { return name; }

    @Override
    public void onTurn(Game game) {
        // 本地玩家通常等待鼠标点击，这里可以通知UI高亮显示“请下棋”
        System.out.println("轮到本地玩家 " + name + " 下棋了");
    }
}