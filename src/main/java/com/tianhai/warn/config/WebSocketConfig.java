package com.tianhai.warn.config;

import com.tianhai.warn.handler.LocationWebSocketHandler;
import com.tianhai.warn.handler.MediaWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private LocationWebSocketHandler locationWebSocketHandler;

    @Autowired
    private MediaWebSocketHandler mediaWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(locationWebSocketHandler, "/ws/location")
                .setAllowedOrigins("*"); // 在生产环境中应该限制允许的源

        registry.addHandler(mediaWebSocketHandler, "/ws/media")
                .setAllowedOrigins("*"); // 音视频流WebSocket端点
    }
}
