package com.heypixel.heypixelmod.obsoverlay.values.impl;

import com.heypixel.heypixelmod.obsoverlay.values.HasValue;
import com.heypixel.heypixelmod.obsoverlay.values.Value;
import com.heypixel.heypixelmod.obsoverlay.values.ValueType;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ModeValue extends Value {
    private final String[] values;
    private final Consumer<Value> update;
    private int currentValue;

    public ModeValue(HasValue key, String name, String[] values, int defaultValue, Consumer<Value> update, Supplier<Boolean> visibility) {
        super(key, name, visibility);
        this.update = update;
        // 确保values数组不为空（避免空指针）
        this.values = values != null ? values : new String[0];
        // 初始化时就对默认值进行边界检查
        this.currentValue = clamp(defaultValue, this.values);
    }

    public boolean isCurrentMode(String mode) {
        return this.getCurrentMode().equalsIgnoreCase(mode);
    }

    @Override
    public ValueType getValueType() {
        return ValueType.MODE;
    }

    @Override
    public ModeValue getModeValue() {
        return this;
    }

    public String getCurrentMode() {
        // 每次获取时再次检查索引（防止意外修改导致越界）
        currentValue = clamp(currentValue, values);
        // 处理空数组的极端情况
        return values.length > 0 ? values[currentValue] : "";
    }

    public void setCurrentValue(int currentValue) {
        // 设置值时强制限制在有效范围内
        int clamped = clamp(currentValue, values);
        if (this.currentValue != clamped) {
            this.currentValue = clamped;
            if (this.update != null) {
                this.update.accept(this);
            }
        }
    }

    // 新增：切换到下一个模式（自动循环）
    public void nextMode() {
        setCurrentValue(currentValue + 1);
    }

    // 新增：切换到上一个模式（自动循环）
    public void prevMode() {
        setCurrentValue(currentValue - 1);
    }

    // 工具方法：将索引限制在有效范围内（0 到 length-1）
    private int clamp(int index, String[] array) {
        if (array.length == 0) return 0; // 空数组特殊处理
        // 确保索引不小于0，且不大于数组长度-1
        return Math.max(0, Math.min(index, array.length - 1));
    }

    public String[] getValues() {
        return this.values;
    }

    public Consumer<Value> getUpdate() {
        return this.update;
    }

    public int getCurrentValue() {
        // 返回前再次确认索引有效
        return clamp(currentValue, values);
    }
}