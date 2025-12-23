module gobang.game {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.desktop;

    // 允许 FXML 反射加载 UI 包
    opens gobang.ui to javafx.fxml;
    opens gobang.network to javafx.fxml;

    // 导出你所有的业务子包
    exports gobang.ui;
    exports gobang.network;
    exports gobang.ai;
    exports gobang.core;
    exports gobang.util;
}