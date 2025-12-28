module com.gobang.game {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.desktop;
    requires javafx.graphics;

    opens com.gobang.client.controller to javafx.fxml;
    opens com.gobang.common.model;
    opens com.gobang.server to javafx.fxml;
    opens com.gobang to javafx.fxml,javafx.graphics;
    opens com.gobang.common.network to javafx.fxml;

    exports com.gobang.common.model;
    exports com.gobang.common.network;
    exports com.gobang.common.logic;
    exports com.gobang.client.player;
    exports com.gobang.client.service;
    exports com.gobang.client.ui;
    exports com.gobang;
    exports com.gobang.server;
    exports com.gobang.server.manager;
    exports com.gobang.client;

}