package com.heypixel.heypixelmod.obsoverlay.events.api.events;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * BackTrackPacketEventManager - 处理BackTrack模块的数据包事件
 * 基于LiquidBounce的BacktrackPacketManager适配
 */
public class BackTrackPacketEventManager {
    
    private static final BackTrackPacketEventManager INSTANCE = new BackTrackPacketEventManager();
    
    // 数据包队列
    private final Queue<Packet<?>> delayedPacketQueue = new ConcurrentLinkedQueue<>();
    private final Queue<Packet<?>> packetProcessQueue = new ConcurrentLinkedQueue<>();
    
    // 状态变量
    private boolean shouldCancelPackets = false;
    private int currentDelay = 100;
    private long lastProcessTime = 0;
    
    private final Minecraft mc = Minecraft.getInstance();
    
    private BackTrackPacketEventManager() {
        // 单例模式
    }
    
    public void register() {
        Naven.getInstance().getEventManager().register(this);
    }
    
    public void unregister() {
        Naven.getInstance().getEventManager().unregister(this);
    }
    
    public static BackTrackPacketEventManager getInstance() {
        return INSTANCE;
    }
    
    @EventTarget
    public void onPacket(EventPacket event) {
        if (event.getType() != EventType.RECEIVE || event.isCancelled()) {
            return;
        }
        
        if (!shouldCancelPackets && delayedPacketQueue.isEmpty()) {
            return;
        }
        
        Packet<?> packet = event.getPacket();
        
        // 处理特定包类型
        if (packet instanceof ClientboundPlayerPositionPacket || 
            packet instanceof ClientboundDisconnectPacket) {
            clear(true);
            return;
        }
        
        if (packet instanceof ClientboundSoundPacket soundPacket) {
            if (soundPacket.getSound().value() == SoundEvents.PLAYER_HURT) {
                return;
            }
        }
        
        // 拦截包并添加到延迟队列
        event.setCancelled(true);
        delayedPacketQueue.add(packet);
    }
    
    @EventTarget
    public void onTick(EventRunTicks event) {
        if (mc.player == null || mc.level == null) {
            clear(true);
            return;
        }
        
        // 处理包队列
        if (shouldCancelPackets) {
            processPackets();
        } else {
            clear();
        }
        
        // 处理待处理的包
        processPacketQueue();
        
        // 更新处理时间
        lastProcessTime = System.currentTimeMillis();
    }
    
    private void processPackets() {
        delayedPacketQueue.removeIf(packet -> {
            if (System.currentTimeMillis() - lastProcessTime >= currentDelay) {
                packetProcessQueue.add(packet);
                return true;
            }
            return false;
        });
    }
    
    private void processPacketQueue() {
        packetProcessQueue.removeIf(packet -> {
            if (mc.getConnection() != null) {
                try {
                    // 处理包
                    ((Packet) packet).handle(mc.getConnection());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        });
    }
    
    public void clear(boolean handlePackets) {
        if (handlePackets) {
            delayedPacketQueue.forEach(packet -> {
                if (mc.getConnection() != null) {
                    try {
                        ((Packet) packet).handle(mc.getConnection());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        
        delayedPacketQueue.clear();
        packetProcessQueue.clear();
        shouldCancelPackets = false;
        lastProcessTime = System.currentTimeMillis();
    }
    
    public void clear() {
        clear(true);
    }
    
    public void setShouldCancelPackets(boolean shouldCancelPackets) {
        this.shouldCancelPackets = shouldCancelPackets;
        if (!shouldCancelPackets) {
            clear();
        }
    }
    
    public boolean isShouldCancelPackets() {
        return shouldCancelPackets;
    }
    
    public void setCurrentDelay(int currentDelay) {
        this.currentDelay = currentDelay;
    }
    
    public int getCurrentDelay() {
        return currentDelay;
    }
    
    public boolean isProcessingPackets() {
        return !delayedPacketQueue.isEmpty() || !packetProcessQueue.isEmpty();
    }
    
    public int getQueuedPacketCount() {
        return delayedPacketQueue.size() + packetProcessQueue.size();
    }
}