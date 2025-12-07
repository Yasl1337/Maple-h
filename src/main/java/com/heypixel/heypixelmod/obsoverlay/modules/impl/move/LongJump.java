package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.Notification;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationLevel;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.MoveUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.Rotation;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

@ModuleInfo(
        name = "LongJump",
        category = Category.MOVEMENT,
        description = "Allows you to use fireball longjump"
)
public class LongJump extends Module {
   public static Rotation rotation = null;
   public static LongJump instance;
   private final SmoothAnimationTimer progress = new SmoothAnimationTimer(0.0F, 0.0F, 0.2F);
   private static final int mainColor = (new Color(150, 45, 45, 255)).getRGB();
   private static final int backgroundColor = Integer.MIN_VALUE;

   private boolean notMoving = false;
   private boolean enabled = false;
   private int rotateTick = 0;
   private int lastSlot = -1;
   private boolean delayed = false;
   private boolean shouldDisableAndRelease = false;
   private boolean isUsingItem = false;
   private boolean mouse4Pressed = false;
   private boolean mouse5Pressed = false;

   private int usedFireballCount = 0;
   private int receivedKnockbacks = 0;
   private int initialFireballCount = 0;
   private int releasedKnockbacks = 0;
   private final List<Integer> knockbackPositions = new ArrayList<>();
   private final LinkedBlockingQueue<Packet<?>> packets = new LinkedBlockingQueue<>();

   public LongJump() {
      instance = this;
   }

   private void releaseAll() {
      while(!this.packets.isEmpty()) {
         try {
            Packet<?> packet = this.packets.poll();
            if (packet != null && mc.getConnection() != null) {
               @SuppressWarnings("unchecked")
               Packet<ClientPacketListener> typedPacket = (Packet<ClientPacketListener>) packet;
               typedPacket.handle(mc.getConnection());
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   private void releaseToKnockback(int knockbackIndex) {
      if (knockbackIndex < this.knockbackPositions.size()) {
         int targetPosition = this.knockbackPositions.get(knockbackIndex);
         int releasedCount = 0;

         while(!this.packets.isEmpty() && releasedCount <= targetPosition) {
            try {
               Packet<?> packet = this.packets.poll();
               if (packet != null && mc.getConnection() != null) {
                  @SuppressWarnings("unchecked")
                  Packet<ClientPacketListener> typedPacket = (Packet<ClientPacketListener>) packet;
                  typedPacket.handle(mc.getConnection());
               }
               ++releasedCount;
            } catch (Exception e) {
               e.printStackTrace();
            }
         }

         for(int i = knockbackIndex + 1; i < this.knockbackPositions.size(); ++i) {
            this.knockbackPositions.set(i, this.knockbackPositions.get(i) - (targetPosition + 1));
         }
      }
   }

   private void updateProgressBar() {
      if (this.receivedKnockbacks == 0) {
         this.progress.target = 0.0F;
      } else {
         float remainingKnockbacks = (float)(this.receivedKnockbacks - this.releasedKnockbacks);
         this.progress.target = Mth.clamp(remainingKnockbacks / (float)this.receivedKnockbacks * 100.0F, 0.0F, 100.0F);
      }
   }

   private int getFireballSlot() {
      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (!stack.isEmpty() && stack.is(Items.FIRE_CHARGE)) {
            return i;
         }
      }
      return -1;
   }

   private int getFireballCount() {
      int count = 0;
      for(int i = 0; i < 9; ++i) {
         ItemStack itemStack = mc.player.getInventory().getItem(i);
         if (itemStack.is(Items.FIRE_CHARGE)) {
            count += itemStack.getCount();
         }
      }
      return count;
   }

   private int setupFireballSlot() {
      int fireballSlot = this.getFireballSlot();
      if (fireballSlot == -1) {
         Naven.getInstance().getNotificationManager().addNotification(
                 Notification.create("No FireBall Found!", false)
         );
         this.setEnabled(false);
      }
      return fireballSlot;
   }

   public void onEnable() {
      this.resetState();
      ChatUtils.addChatMessage("§aLongJump enabled! Press Mouse4 to jump & use fireball, Mouse5 to release each knockback");
   }

   public void onDisable() {
      this.releaseAll();
      if (this.lastSlot != -1 && mc.player != null) {
         mc.player.getInventory().selected = this.lastSlot;
      }
      if (mc.options != null) {
         mc.options.keyUse.setDown(false);
         mc.options.keyJump.setDown(false);
      }
      this.resetState();
      super.onDisable();
   }

   private void resetState() {
      this.releaseAll();
      this.rotateTick = 0;
      this.enabled = true;
      this.lastSlot = -1;
      this.notMoving = false;
      this.delayed = false;
      this.isUsingItem = false;
      rotation = null;
      this.shouldDisableAndRelease = false;
      this.mouse4Pressed = false;
      this.mouse5Pressed = false;
      this.usedFireballCount = 0;
      this.receivedKnockbacks = 0;
      this.initialFireballCount = 0;
      this.releasedKnockbacks = 0;
      this.knockbackPositions.clear();
      this.progress.target = 0.0F;
      this.progress.value = 0.0F;
   }

   @EventTarget
   public void onUpdate(EventUpdate event) {
      if (!this.isEnabled()) return;

      if (this.shouldDisableAndRelease) {
         this.setEnabled(false);
         return;
      }

      if (this.enabled) {
         if (!MoveUtils.isMoving()) this.notMoving = true;
         this.enabled = false;
      }

      handleMouseInput();
   }

   private void handleMouseInput() {
      long window = mc.getWindow().getWindow();
      boolean currentMouse4 = GLFW.glfwGetMouseButton(window, 3) == 1;
      if (currentMouse4 && !this.mouse4Pressed) {
         this.mouse4Pressed = true;
         if (!this.isUsingItem && this.rotateTick == 0) {
            int fireballSlot = this.setupFireballSlot();
            if (fireballSlot != -1) {
               this.lastSlot = mc.player.getInventory().selected;
               mc.player.getInventory().selected = fireballSlot;
               this.rotateTick = 1;
               Naven.getInstance().getNotificationManager().addNotification(
                       Notification.create("§eStarting fireball usage #" + (this.usedFireballCount + 1), true)
               );
            }
         }
      } else if (!currentMouse4) {
         this.mouse4Pressed = false;
      }

      boolean currentMouse5 = GLFW.glfwGetMouseButton(window, 4) == 1;
      if (currentMouse5 && !this.mouse5Pressed) {
         this.mouse5Pressed = true;
         if (this.delayed && this.releasedKnockbacks < this.receivedKnockbacks) {
            Naven.getInstance().getNotificationManager().addNotification(
                    Notification.create("§aReleasing " + (this.releasedKnockbacks + 1) + "/" + this.receivedKnockbacks, true)
            );
            this.releaseToKnockback(this.releasedKnockbacks);
            ++this.releasedKnockbacks;
            this.updateProgressBar();
            if (this.releasedKnockbacks >= this.receivedKnockbacks) {
               this.delayed = false;
               this.setEnabled(false);
            }
         } else if (!this.delayed) {
            Naven.getInstance().getNotificationManager().addNotification(
                    Notification.create("No intercepted packets", false)
            );
            this.setEnabled(false);
         } else {
            ChatUtils.addChatMessage("§cAll knockbacks already released");
         }
      } else if (!currentMouse5) {
         this.mouse5Pressed = false;
      }
   }

   @EventTarget
   public void onRender2D(EventRender2D event) {
      if (!this.isEnabled()) return;

      int screenWidth = mc.getWindow().getGuiScaledWidth();
      int screenHeight = mc.getWindow().getGuiScaledHeight();
      int progressX = screenWidth / 2 - 60;
      int progressY = screenHeight / 2 + 35;
      int progressWidth = 120;
      int progressHeight = 6;

      this.progress.update(true);
      RenderUtils.drawRoundedRect(event.getStack(), (float)progressX, (float)progressY, (float)progressWidth, (float)progressHeight, 2.0F, backgroundColor);

      String progressText;
      if (this.receivedKnockbacks > 0) {
         this.updateProgressBar();
         float progressFill = this.progress.value / 100.0F * (float)progressWidth;
         if (progressFill > 0.0F) {
            RenderUtils.drawRoundedRect(event.getStack(), (float)progressX, (float)progressY, progressFill, (float)progressHeight, 2.0F, mainColor);
         }
         progressText = String.format("§fKnockbacks: %d/%d", this.receivedKnockbacks - this.releasedKnockbacks, this.receivedKnockbacks);
      } else {
         progressText = "Waiting for knockback...";
      }

      float progressTextX = (float)screenWidth / 2.0F - (float)mc.font.width(progressText) / 2.0F;
      float progressTextY = (float)(progressY + progressHeight + 6);
      Fonts.opensans.render(event.getStack(), progressText, (int)progressTextX, (int)progressTextY, Color.WHITE, true, 0.4);
   }

   @EventTarget
   public void onPacket(EventPacket event) {
      if (!this.isEnabled() || mc.level == null) {
         if (this.delayed) {
            mc.execute(() -> {
               this.releaseAll();
               this.delayed = false;
            });
         }
         return;
      }

      Packet<?> packet = event.getPacket();
      if (this.delayed && event.getType() == EventType.RECEIVE) {
         if (packet instanceof ClientboundPlayerPositionPacket) {
            this.shouldDisableAndRelease = true;
            event.setCancelled(true);
         } else {
            if (packet instanceof ClientboundSetEntityMotionPacket motionPacket && motionPacket.getId() == mc.player.getId()) {
               ++this.receivedKnockbacks;
               this.knockbackPositions.add(this.packets.size());
               this.updateProgressBar();
               mc.execute(() -> ChatUtils.addChatMessage("§eKnockback #" + this.receivedKnockbacks + " received"));
            }
            event.setCancelled(true);
            this.packets.add(packet);
         }
      } else if (packet instanceof ClientboundSetEntityMotionPacket motionPacket &&
              event.getType() == EventType.RECEIVE &&
              motionPacket.getId() == mc.player.getId() &&
              this.usedFireballCount > 0 && !this.delayed) {
         ++this.receivedKnockbacks;
         this.knockbackPositions.add(this.packets.size());
         mc.execute(() -> ChatUtils.addChatMessage("§eReceived #" + this.receivedKnockbacks + ", starting packet interception"));
         event.setCancelled(true);
         this.packets.add(packet);
         this.delayed = true;
         this.updateProgressBar();
         mc.execute(() -> ChatUtils.addChatMessage("§ePacket interception started, press Mouse5 to release each"));
      }
   }

   @EventTarget
   public void onMotion(EventMotion event) {
      if (!this.isEnabled()) return;

      if (event.getType() == EventType.PRE) {
         if (this.rotateTick > 0) {
            if (this.rotateTick == 1) {
               ++this.usedFireballCount;
               ChatUtils.addChatMessage("§aJumping for fireball #" + this.usedFireballCount);
               mc.options.keyJump.setDown(true);

               float yaw = this.notMoving ? mc.player.getYRot() : mc.player.getYRot() - 180.0F;
               float pitch = this.notMoving ? 90.0F : 88.0F;
               rotation = new Rotation(yaw, pitch);
            }

            if (this.rotateTick >= 2) {
               this.rotateTick = 0;
               int fireballSlot = this.setupFireballSlot();
               if (fireballSlot != -1) {
                  mc.player.getInventory().selected = fireballSlot;
                  this.initialFireballCount = this.getFireballCount();
                  mc.options.keyUse.setDown(true);
                  this.isUsingItem = true;
                  ChatUtils.addChatMessage("§eUsing fireball #" + this.usedFireballCount + ", initial count: " + this.initialFireballCount);
               } else {
                  this.setEnabled(false);
               }
            }

            if (this.rotateTick != 0) {
               ++this.rotateTick;
            }
         }
      } else {
         if (this.isUsingItem) {
            int currentFireballCount = this.getFireballCount();
            if (currentFireballCount < this.initialFireballCount || this.getFireballSlot() == -1) {
               mc.options.keyUse.setDown(false);
               mc.options.keyJump.setDown(false);
               rotation = null;
               this.isUsingItem = false;
               if (currentFireballCount < this.initialFireballCount) {
                  ChatUtils.addChatMessage("§eFireball #" + this.usedFireballCount + " used! Waiting for next input");
               } else {
                  ChatUtils.addChatMessage("§cNo more fireballs available!");
               }
            }
         }
      }
   }
}