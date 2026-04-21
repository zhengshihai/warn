package com.tianhai.warn.handler;

import com.alibaba.fastjson.JSONObject;
import com.tianhai.warn.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// 处理通过websocket一键报警的音视频数据
@Component
public class MediaWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(MediaWebSocketHandler.class);

    private static final Map<String, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();

    private static final Map<String, Long> LAST_ACTIVE_TIME = new ConcurrentHashMap<>();

    // 记录已经触发过合并的sessionId，防止重复处理
    private static final Map<String, Boolean> PROCESSED_SESSIONS = new ConcurrentHashMap<>();

    private static final int MAX_MESSAGE_SIZE = 10 * 1024 * 1024; // 10MB

    private static final int SESSION_TIMEOUT = 5 * 60; // 5分钟

    @Autowired
    private FileService fileService;

    // 心跳检查
    public MediaWebSocketHandler() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::checkHeartbeats, 30, 30, TimeUnit.SECONDS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String alarmNo = getAlarmNoFromSession(session);
        if (alarmNo == null) {
            logger.error("WebSocket连接失败，缺少alarmNo参数, URI: {}", session.getUri());
            session.close(CloseStatus.BAD_DATA.withReason("缺少参数alarmNo"));
            return;
        }
        session.setBinaryMessageSizeLimit(MAX_MESSAGE_SIZE);
        SESSIONS.put(alarmNo, session);
        LAST_ACTIVE_TIME.put(alarmNo, System.currentTimeMillis());

        logger.info("音视频WebSocket连接已建立，alarmNo:{}, 远程地址：{}, URI:{}",
                alarmNo, session.getRemoteAddress(), session.getUri());
    }

    // 处理音视频的二进制数据
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        // 此处BinaryMessage的二进制结构
        /*
         * ┌──────────────┬─────────────────────┬────────────────────────┐
         * │ HeaderLength │ Header(JSON String) │ Chunk (音视频二进制) |
         * │ (4B) │ (N bytes) │ (剩余全部 Byte) |
         * └──────────────┴─────────────────────┴────────────────────────┘
         */
        ByteBuffer buffer = message.getPayload();
        buffer.mark();

        // 读取头部长度
        // 1. 读取头部长度
        if (buffer.remaining() < 4) {
            logger.error("收到的分片数据不足4字节，无法解析头部长度");
            session.sendMessage(new TextMessage("收到的分片数据不足4字节"));
            return;
        }
        int headerLen = buffer.getInt();
        if (buffer.remaining() < headerLen) {
            logger.error("收到的分片数据不足headerLen，无法解析头部, headerLen={}, buffer.remaining={}",
                    headerLen, buffer.remaining());
            session.sendMessage(new TextMessage("收到的分片数据不足headerLen"));
            return;
        }
        byte[] headerBytes = new byte[headerLen];
        buffer.get(headerBytes);

        String headerJson = new String(headerBytes, StandardCharsets.UTF_8);
        JSONObject header = JSONObject.parseObject(headerJson);

        String alarmNo = header.getString("alarmNo");
        // todo 这里可以考虑使用studentNo
        String sessionId = header.getString("sessionId");
        int chunkIndex = header.getIntValue("chunkIndex");
        boolean isLastChunk = header.getBooleanValue("isLastChunk");

        // 获取剩余的音视频分片数据
        ByteBuffer chunkData = buffer.slice();

        // 保存二进制音视频数据为chunk文件
        fileService.saveMediaChunk(alarmNo, sessionId, chunkIndex, chunkData);

        // 向前端返回ACK确认信号
        JSONObject resp = new JSONObject();
        resp.put("type", "ACK");
        resp.put("chunkIndex", chunkIndex);
        session.sendMessage(new TextMessage(resp.toJSONString()));

        // 如果是最后一个音视频分片数据，则触发合并（防止重复处理）
        if (isLastChunk) {
            String sessionKey = alarmNo + "_" + sessionId;
            // 使用putIfAbsent确保只有第一次才会触发合并
            Boolean alreadyProcessed = PROCESSED_SESSIONS.putIfAbsent(sessionKey, true);
            if (alreadyProcessed == null) {
                // 首次处理，触发合并
                logger.info("首次收到最后一个分片，开始合并音视频数据: alarmNo={}, sessionId={}", alarmNo, sessionId);
                fileService.mergeMediaChunks(alarmNo, sessionId);
            } else {
                // 已经处理过，跳过
                logger.warn("重复收到最后一个分片，跳过合并操作，防止重复处理: alarmNo={}, sessionId={}", alarmNo, sessionId);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String alarmNo = getAlarmNoFromSession(session);
        if (alarmNo != null) {
            SESSIONS.remove(alarmNo);
            LAST_ACTIVE_TIME.remove(alarmNo);

            // 清理该alarmNo相关的所有已处理记录
            String prefix = alarmNo + "_";
            PROCESSED_SESSIONS.keySet().removeIf(key -> key.startsWith(prefix));
            logger.debug("已清理alarmNo={}相关的已处理记录", alarmNo);

            logger.info("音视频WebSocket连接已关闭，alarmNo: {}, 远程地址：{}, 原因: {}",
                    alarmNo, session.getRemoteAddress(), status.getReason());
        } else {
            logger.warn("音视频WebSocket连接关闭，但未找到对应的alarmNo，远程地址：{}, 原因: {}",
                    session.getRemoteAddress(), status.getReason());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("音视频WebSocket传输错误，远程地址：{}, 错误信息: {}",
                session.getRemoteAddress(), exception.getMessage());
        String alarmNo = getAlarmNoFromSession(session);
        if (alarmNo != null) {
            SESSIONS.remove(alarmNo);
            LAST_ACTIVE_TIME.remove(alarmNo);
            logger.info("已移除错误连接的session，alarmNo: {}", alarmNo);
        }
        session.close(CloseStatus.SERVER_ERROR);
    }

    // 获取一键报警的业务报警号alarmNo
    private String getAlarmNoFromSession(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            if (query == null || !query.contains("alarmNo=")) {
                return null;
            }
            return query.split("alarmNo=")[1].split("&")[0];
        } catch (Exception e) {
            logger.error("获取alarmNo失败，URI: {} ", session.getUri(), e);
            return null;
        }
    }

    // 心跳检查
    private void checkHeartbeats() {
        long now = System.currentTimeMillis();
        SESSIONS.forEach((alarmNo, session) -> {
            Long lastActive = LAST_ACTIVE_TIME.get(alarmNo);
            if (lastActive != null && now - lastActive > SESSION_TIMEOUT * 1000) {
                try {
                    session.close(CloseStatus.GOING_AWAY.withReason("Session timeout"));
                    SESSIONS.remove(alarmNo);
                    LAST_ACTIVE_TIME.remove(alarmNo);
                    logger.info("关闭连接超时的音视频会话，alarmNo: {}, 远程地址：{}", alarmNo, session.getRemoteAddress());
                } catch (Exception e) {
                    logger.error("关闭超时连接失败，alarmNo: {}", alarmNo, e);
                }
            }
        });
    }

}
