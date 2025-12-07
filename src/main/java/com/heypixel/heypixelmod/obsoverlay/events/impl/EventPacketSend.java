package com.heypixel.heypixelmod.obsoverlay.events.impl;

// 导入你已有的 Event 接口（包路径要和你实际的 Event.java 一致）
import com.heypixel.heypixelmod.obsoverlay.events.api.events.Event;
import net.minecraft.network.protocol.Packet;

/**
 * 数据包发送事件
 */
public class EventPacketSend implements Event {  // 关键：把 extends 改为 implements
    private final Packet<?> packet;
    private boolean cancelled;  // 内部维护取消状态

    public EventPacketSend(Packet<?> packet) {
        this.packet = packet;
        this.cancelled = false;  // 初始化：默认不取消
    }

    // 获取当前发送的数据包
    public Packet<?> getPacket() {
        return packet;
    }

    // 取消事件的方法（补全接口缺失的实现）
    public void cancel() {
        this.cancelled = true;
    }

    // 检查事件是否被取消（补全接口缺失的实现）
    public boolean isCancelled() {
        return cancelled;
    }
}