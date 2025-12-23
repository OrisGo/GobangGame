package gobang.network;

import gobang.util.Constant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 双客户端中转服务端（支持黑棋、白棋两个客户端连接）
 */
public class Server {
    private ServerSocket serverSocket;
    private Socket blackSocket; // 黑棋客户端
    private Socket whiteSocket; // 白棋客户端
    private BufferedReader blackIn;
    private OutputStreamWriter blackOut;
    private BufferedReader whiteIn;
    private OutputStreamWriter whiteOut;
    private boolean isRunning = false;

    /**
     * 启动服务端，等待黑棋、白棋客户端连接
     */
    public void start() {
        new Thread(() -> {
            try {
                // 绑定端口
                serverSocket = new ServerSocket(Constant.PORT);
                isRunning = true;
                System.out.println("服务端已启动，端口：" + Constant.PORT + "，等待玩家连接...");

                // 第一步：等待黑棋客户端连接
                blackSocket = serverSocket.accept();
                System.out.println("黑棋玩家已连接：" + blackSocket.getInetAddress());
                blackIn = new BufferedReader(new InputStreamReader(blackSocket.getInputStream()));
                blackOut = new OutputStreamWriter(blackSocket.getOutputStream());

                // 第二步：等待白棋客户端连接
                whiteSocket = serverSocket.accept();
                System.out.println("白棋玩家已连接：" + whiteSocket.getInetAddress());
                whiteIn = new BufferedReader(new InputStreamReader(whiteSocket.getInputStream()));
                whiteOut = new OutputStreamWriter(whiteSocket.getOutputStream());

                // 启动消息转发线程：黑棋消息转发给白棋
                new Thread(() -> forwardMessage(blackIn, whiteOut, "黑棋")).start();
                // 启动消息转发线程：白棋消息转发给黑棋
                new Thread(() -> forwardMessage(whiteIn, blackOut, "白棋")).start();

            } catch (IOException e) {
                e.printStackTrace();
                stop();
            }
        }).start();
    }

    /**
     * 转发消息：从一个客户端读取消息，发送到另一个客户端
     * @param in 读取消息的输入流
     * @param out 发送消息的输出流
     * @param player 发送方标识
     */
    private void forwardMessage(BufferedReader in, OutputStreamWriter out, String player) {
        String msgStr;
        try {
            while (isRunning && (msgStr = in.readLine()) != null) {
                System.out.println("收到" + player + "消息：" + msgStr);
                // 转发消息（添加换行符，确保客户端能读取）
                out.write(msgStr + "\n");
                out.flush();
            }
        } catch (IOException e) {
            System.out.println(player + "断开连接，停止转发消息");
            e.printStackTrace();
        }
    }

    /**
     * 停止服务端
     */
    public void stop() {
        isRunning = false;
        try {
            // 关闭所有流和套接字
            if (blackIn != null) blackIn.close();
            if (blackOut != null) blackOut.close();
            if (blackSocket != null) blackSocket.close();
            if (whiteIn != null) whiteIn.close();
            if (whiteOut != null) whiteOut.close();
            if (whiteSocket != null) whiteSocket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("服务端已停止");
    }

    public boolean isRunning() {
        return isRunning;
    }
}