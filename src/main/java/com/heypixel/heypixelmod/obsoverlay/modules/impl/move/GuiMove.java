package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventKey;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;

import java.util.HashMap;
import java.util.Map;

@ModuleInfo(
        name = "GuiMove",
        description = "Allows you to walk while a GUI screen is opened",
        category = Category.MOVEMENT
)
public class GuiMove extends Module {
    private final Minecraft mc = Minecraft.getInstance();

    // 配置项
    private final ModeValue behavior = ValueBuilder.create(this, "Behavior")
            .setDefaultModeIndex(0)
            .setModes("Normal", "Undetectable")
            .build()
            .getModeValue();

    private final BooleanValue passthroughSneak = ValueBuilder.create(this, "PassthroughSneak")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private final BooleanValue passthroughJump = ValueBuilder.create(this, "PassthroughJump")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private final BooleanValue noSprint = ValueBuilder.create(this, "NoSprint")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    // 移动按键状态映射
    private final Map<KeyMapping, Boolean> movementKeys = new HashMap<>();
    private KeyMapping[] movementKeyArray;

    public GuiMove() {}

    /**
     * 初始化移动按键列表
     */
    private void initializeMovementKeys() {
        if (mc.options == null) return;

        movementKeys.put(mc.options.keyUp, false);
        movementKeys.put(mc.options.keyDown, false);
        movementKeys.put(mc.options.keyLeft, false);
        movementKeys.put(mc.options.keyRight, false);
        movementKeys.put(mc.options.keyJump, false);
        movementKeys.put(mc.options.keyShift, false);
        movementKeys.put(mc.options.keySprint, false);

        movementKeyArray = new KeyMapping[]{
                mc.options.keyUp, mc.options.keyDown,
                mc.options.keyLeft, mc.options.keyRight,
                mc.options.keyJump, mc.options.keyShift,
                mc.options.keySprint
        };
    }

    @EventTarget
    private void onKey(EventKey event) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        KeyMapping key = getMovementKeyByCode(event.getKey());
        if (key == null) return;

        if (shouldHandleInputs(key)) {
            boolean pressed = event.isState();
            movementKeys.put(key, pressed);

            if (isEnabled()) {
                updateKeyBindingState(key, pressed);
            }
        }
    }

    @EventTarget
    private void onMoveInput(EventMoveInput event) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        Screen currentScreen = mc.screen;

        if (currentScreen == null || !shouldHandleMoveInput()) {
            return;
        }

        // 获取各按键状态
        boolean upPressed = Boolean.TRUE.equals(movementKeys.get(mc.options.keyUp));
        boolean downPressed = Boolean.TRUE.equals(movementKeys.get(mc.options.keyDown));
        boolean leftPressed = Boolean.TRUE.equals(movementKeys.get(mc.options.keyLeft));
        boolean rightPressed = Boolean.TRUE.equals(movementKeys.get(mc.options.keyRight));
        boolean jumpPressed = Boolean.TRUE.equals(movementKeys.get(mc.options.keyJump));
        boolean sneakPressed = Boolean.TRUE.equals(movementKeys.get(mc.options.keyShift));
        boolean sprintPressed = Boolean.TRUE.equals(movementKeys.get(mc.options.keySprint));

        // 处理前后移动
        if (upPressed) {
            event.setForward(1.0f);
        } else if (downPressed) {
            event.setForward(-1.0f);
        }

        // 处理左右移动
        if (leftPressed) {
            event.setStrafe(1.0f);
        } else if (rightPressed) {
            event.setStrafe(-1.0f);
        }

        // 处理跳跃
        if (shouldHandleJumpInput()) {
            event.setJump(jumpPressed);
        }

        // 处理潜行
        if (shouldHandleSneakInput()) {
            event.setSneak(sneakPressed);
        }

        // 处理 sprint（通过直接控制玩家状态，而非事件）
        if (noSprint.getCurrentValue()) {
            if (player.isSprinting()) {
                player.setSprinting(false); // 强制关闭 sprint
            }
        } else if (sprintPressed && !player.isSprinting()) {
            player.setSprinting(true); // 强制开启 sprint
        }
    }

    /**
     * 判断是否应该处理移动输入
     */
    private boolean shouldHandleMoveInput() {
        Screen screen = mc.screen;
        if (screen == null) {
            return false;
        }

        // 聊天界面不处理
        if (screen instanceof ChatScreen) {
            return false;
        }

        // 创造模式搜索框不处理
        if (isInCreativeSearchField(screen)) {
            return false;
        }

        // 根据模式判断
        return switch (behavior.getCurrentMode()) {
            case "Normal" -> true;
            case "Undetectable" -> !(screen instanceof AbstractContainerScreen);
            default -> false;
        };
    }

    /**
     * 判断是否应该处理按键输入
     */
    private boolean shouldHandleInputs(KeyMapping keyBinding) {
        Screen screen = mc.screen;
        if (screen == null) {
            return true;
        }

        // 聊天界面不处理
        if (screen instanceof ChatScreen) {
            return false;
        }

        // 创造模式搜索框不处理
        if (isInCreativeSearchField(screen)) {
            return false;
        }

        // 潜行键过滤
        if (keyBinding == mc.options.keyShift && !passthroughSneak.getCurrentValue()) {
            return false;
        }

        // 跳跃键过滤
        if (keyBinding == mc.options.keyJump && !passthroughJump.getCurrentValue()) {
            return false;
        }

        // 根据模式判断
        return switch (behavior.getCurrentMode()) {
            case "Normal" -> true;
            case "Undetectable" -> !(screen instanceof AbstractContainerScreen);
            default -> false;
        };
    }

    private boolean shouldHandleSneakInput() {
        return passthroughSneak.getCurrentValue();
    }

    private boolean shouldHandleJumpInput() {
        return passthroughJump.getCurrentValue();
    }

    /**
     * 通过按键代码获取对应的移动按键
     */
    private KeyMapping getMovementKeyByCode(int keyCode) {
        if (movementKeyArray == null) return null;

        for (KeyMapping key : movementKeyArray) {
            if (key != null && key.matches(keyCode, 0)) {
                return key;
            }
        }
        return null;
    }

    /**
     * 判断是否在创造模式搜索框中
     */
    private boolean isInCreativeSearchField(Screen screen) {
        if (!(screen instanceof CreativeModeInventoryScreen)) {
            return false;
        }

        try {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 更新按键绑定状态
     */
    private void updateKeyBindingState(KeyMapping key, boolean pressed) {
        if (key != null) {
            key.setDown(pressed);
        }
    }

    @Override
    public void onEnable() {
        initializeMovementKeys();

        // 重置按键状态
        Map<KeyMapping, Boolean> keysCopy = new HashMap<>(movementKeys);
        for (KeyMapping key : keysCopy.keySet()) {
            movementKeys.put(key, false);
        }

        // 处理 sprint 状态
        LocalPlayer player = mc.player;
        if (noSprint.getCurrentValue() && mc.screen != null && player != null && player.isSprinting()) {
            player.setSprinting(false);
        }
    }

    @Override
    public void onDisable() {
        // 重置所有按键状态
        Map<KeyMapping, Boolean> keysCopy = new HashMap<>(movementKeys);
        for (KeyMapping key : keysCopy.keySet()) {
            movementKeys.put(key, false);
            if (key != null) {
                key.setDown(false);
            }
        }
    }

    @Override
    public String getDescription() {
        return "Allows you to walk while a GUI screen is opened with enhanced features";
    }
}