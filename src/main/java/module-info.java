module com.gobang.game {
    // 已有的requires和其他配置...
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.desktop;
    requires javafx.graphics;

    // 关键修改：允许javafx.graphics反射访问主类所在的包
//    opens com.gobang.client to javafx.fxml, javafx.graphics;  // 同时保留对javafx.fxml的开放
    opens com.gobang.client.controller to javafx.fxml;
    opens com.gobang.common.model;

    // 其他已有的opens和exports配置...
    opens com.gobang.common.network to javafx.fxml;

    exports com.gobang.common.model;
    exports com.gobang.common.network;
    exports com.gobang.common.logic;
    exports com.gobang.client.player;
    exports com.gobang.client.service;
    exports com.gobang.client.ui;
    exports com.gobang;
}