package com.trexwallet.posts.service;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PostSocket {
    @Value("${trex.socketio.port:9092}") private int port;
    private SocketIOServer server;
    @PostConstruct public void start() {
        Configuration config = new Configuration();
        config.setHostname("0.0.0.0"); config.setPort(port);
        config.setOrigin("*"); // Replace with your exact frontend origin in production.
        server = new SocketIOServer(config); server.start();
    }
    public void publish(String event, Object payload) { if (server != null) server.getBroadcastOperations().sendEvent(event, payload); }
    @PreDestroy public void stop() { if (server != null) server.stop(); }
}
