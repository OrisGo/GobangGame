package com.gobang.server.manager;

import com.gobang.server.GameRoom;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// 房间管理器：单例模式，统一管理所有游戏房间
public class RoomManager {
    // 单例实例（双重校验锁，线程安全）
    private static volatile RoomManager instance;
    // 房间映射：roomId -> GameRoom，线程安全
    private final Map<String, GameRoom> roomMap = new ConcurrentHashMap<>();

    // 私有构造，禁止外部实例化
    private RoomManager() {}

    // 获取单例实例
    public static RoomManager getInstance() {
        if (instance == null) {
            synchronized (RoomManager.class) {
                if (instance == null) {
                    instance = new RoomManager();
                }
            }
        }
        return instance;
    }

    // ✅ 核心修复：新增缺失的 findAvailableRoom 方法（返回Optional）
    public Optional<GameRoom> findAvailableRoom() {
        return roomMap.values().stream()
                .filter(room -> !room.isFull()) // 筛选未满的房间
                .findFirst(); // 返回第一个可用房间
    }

    // 创建新房间（生成6位UUID作为房间号）
    public GameRoom createRoom() {
        String roomId = UUID.randomUUID().toString().substring(0, 6);
        GameRoom newRoom = new GameRoom(roomId);
        roomMap.put(roomId, newRoom);
        return newRoom;
    }

    // 根据房间号获取房间
    public GameRoom getRoom(String roomId) {
        return roomMap.get(roomId);
    }

    // 根据房间号移除房间
    public void removeRoom(String roomId) {
        roomMap.remove(roomId);
    }
}