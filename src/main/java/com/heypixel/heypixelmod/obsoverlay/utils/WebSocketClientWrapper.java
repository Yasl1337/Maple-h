package com.heypixel.heypixelmod.obsoverlay.utils;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public abstract class WebSocketClientWrapper extends WebSocketClient {
    
    public WebSocketClientWrapper(URI serverUri) {
        super(serverUri);
    }
    
    @Override
    public abstract void onOpen(ServerHandshake handshake);
    
    @Override
    public abstract void onMessage(String message);
    
    @Override
    public abstract void onClose(int code, String reason, boolean remote);
    
    @Override
    public abstract void onError(Exception ex);

    public void connect() {
        try {
            super.connect();
        } catch (Exception e) {
            onError(e);
        }
    }
    

    public void send(String message) {
        if (isOpen()) {
            super.send(message);
        }
    }
    

    public void close() {
        super.close();
    }

    public boolean isOpen() {
        return super.isOpen();
    }
}