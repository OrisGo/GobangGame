package com.gobang.client.controller; // 包结构可根据实际项目调整

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * 客户端联机连接界面Controller
 * 负责关联ClientConnectView.fxml的控件，暂不实现业务逻辑
 */
public class ClientConnectController {

    public ComboBox comboColor;
    // 注入FXML中的控件（fx:id需与FXML中一致）
    @FXML
    private TextField tfServerIp; // 服务器IP输入框

    @FXML
    private TextField tfServerPort; // 服务器端口输入框

    @FXML
    private TextField tfUserName; // 用户名输入框

    @FXML
    private Button btnConnect; // 连接服务器按钮

    @FXML
    private Button btnDisconnect; // 断开连接按钮

    @FXML
    private Label lblConnectStatus; // 连接状态提示标签

    /**
     * 连接服务器按钮点击事件（空实现）
     */
    @FXML
    private void onConnectBtnClicked() {
        // 后续将实现：获取输入参数、建立与服务端的连接
    }

    /**
     * 断开连接按钮点击事件（空实现）
     */
    @FXML
    private void onDisconnectBtnClicked() {
        // 后续将实现：关闭与服务端的连接、重置界面状态
    }

    /**
     * 初始化方法（FXML加载完成后自动调用，空实现）
     */
    @FXML
    private void initialize() {
        // 后续将实现：设置默认值（如IP默认localhost、端口默认8888）
    }

    public void connect(ActionEvent actionEvent) {
    }

    public void close(ActionEvent actionEvent) {
    }
}