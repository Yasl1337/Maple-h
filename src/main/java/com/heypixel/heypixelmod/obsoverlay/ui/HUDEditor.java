package com.heypixel.heypixelmod.obsoverlay.ui;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.Scoreboard;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class HUDEditor {
    private static final Minecraft mc = Minecraft.getInstance();
    private static HUDEditor instance;

    private final File configFile = new File(mc.gameDirectory, "Naven/hud.cfg");


    private final Map<String, HUDElement> hudElements = new HashMap<>();


    private HUDElement draggingElement = null;
    private double dragStartX = 0;
    private double dragStartY = 0;
    private double elementStartX = 0;
    private double elementStartY = 0;


    private boolean editMode = false;

    public HUDEditor() {
        instance = this;
        initializeHUDElements();
        loadHUDConfig();
        Naven.getInstance().getEventManager().register(this);
    }

    public static HUDEditor getInstance() {
        if (instance == null) {
            instance = new HUDEditor();
        }
        return instance;
    }

    
    private void initializeHUDElements() {

        hudElements.put("watermark", new HUDElement("watermark", "Watermark", 5, 5, 200, 25));


        int screenWidth = mc.getWindow().getGuiScaledWidth();
        hudElements.put("arraylist", new HUDElement("arraylist", "ArrayList",
                screenWidth - 250, 1, 250, 300));


        int screenHeight = mc.getWindow().getGuiScaledHeight();
        hudElements.put("targethud", new HUDElement("targethud", "TargetHUD",
                screenWidth / 2.0F + 10.0F, screenHeight / 2.0F + 10.0F, 160, 50));
        

        hudElements.put("itemscounter", new HUDElement("itemscounter", "Items Counter", 10, 10, 100, 80));
        

        hudElements.put("armorrender", new HUDElement("armorrender", "Armor Render", 
                10, screenHeight - 80, 120, 60));
        



        hudElements.put("scoreboard", new HUDElement("scoreboard", "Scoreboard", 0, 0, 120, 200));
    }

    
    private void loadHUDConfig() {
        Properties properties = new Properties();
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                properties.load(reader);

                for (Map.Entry<String, HUDElement> entry : hudElements.entrySet()) {
                    String name = entry.getKey();
                    HUDElement element = entry.getValue();
                    element.x = Double.parseDouble(properties.getProperty(name + ".x", String.valueOf(element.x)));
                    element.y = Double.parseDouble(properties.getProperty(name + ".y", String.valueOf(element.y)));
                }
            } catch (IOException e) {

                e.printStackTrace();
            }
        }
    }

    
    private void saveHUDConfig() {
        Properties properties = new Properties();

        for (HUDElement element : hudElements.values()) {
            properties.setProperty(element.name + ".x", String.valueOf(element.x));
            properties.setProperty(element.name + ".y", String.valueOf(element.y));
        }
        try {

            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }

            try (FileWriter writer = new FileWriter(configFile)) {
                properties.store(writer, "HUD Elements Positions");
            }
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    
    @EventTarget
    public void onRender2D(EventRender2D event) {

        boolean shouldEdit = mc.screen instanceof ChatScreen;

        if (shouldEdit != editMode) {
            editMode = shouldEdit;
        }


        if (editMode) {
            renderEditMode(event);
        }
    }

    
    private void startDragging(double mouseX, double mouseY) {

        for (HUDElement element : hudElements.values()) {
            boolean isHovering = false;
            

            if (element.name.equals("scoreboard")) {
                Scoreboard scoreboardModule = (Scoreboard)Naven.getInstance().getModuleManager().getModule(Scoreboard.class);
                if (scoreboardModule != null && scoreboardModule.isEnabled()) {
                    float screenX = scoreboardModule.getScreenX();
                    float screenY = scoreboardModule.getScreenY();
                    float width = scoreboardModule.getWidth();
                    float height = scoreboardModule.getHeight();
                    isHovering = mouseX >= screenX && mouseX <= screenX + width && 
                                mouseY >= screenY && mouseY <= screenY + height;
                }
            } else {
                isHovering = element.isHovering(mouseX, mouseY);
            }
            
            if (isHovering) {
                draggingElement = element;
                dragStartX = mouseX;
                dragStartY = mouseY;
                elementStartX = element.x;
                elementStartY = element.y;
                break;
            }
        }
    }

    
    private void stopDragging() {
        if (draggingElement != null) {
            draggingElement = null;
            saveHUDConfig();
        }
    }

    
    private void updateDragging() {
        if (draggingElement != null) {

            double mouseX = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
            double mouseY = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();


            draggingElement.x = elementStartX + (mouseX - dragStartX);
            draggingElement.y = elementStartY + (mouseY - dragStartY);
        }
    }

    
    private void renderEditMode(EventRender2D event) {
        updateDragging();

        double mouseX = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();

        handleMouseInput(mouseX, mouseY);

        CustomTextRenderer font = Fonts.opensans;


        for (HUDElement element : hudElements.values()) {

            if (element.name.equals("scoreboard")) {
                renderScoreboardBorder(event, element, mouseX, mouseY, font);
                continue;
            }
            
            boolean hovering = element.isHovering(mouseX, mouseY);
            boolean shouldDrawBorder = hovering || element == draggingElement;

            if (shouldDrawBorder) {

                int borderColor = element == draggingElement ? Color.RED.getRGB() : Color.YELLOW.getRGB();
                drawElementBorder(event.getStack(), element, borderColor);


                font.render(event.getStack(), element.displayName,
                        element.x + 2, element.y - 12, Color.WHITE, true, 0.3);
            }
        }
    }
    
    
    private void renderScoreboardBorder(EventRender2D event, HUDElement element, double mouseX, double mouseY, CustomTextRenderer font) {
        Scoreboard scoreboardModule = (Scoreboard)Naven.getInstance().getModuleManager().getModule(Scoreboard.class);
        if (scoreboardModule == null || !scoreboardModule.isEnabled()) {
            return;
        }
        

        float screenX = scoreboardModule.getScreenX();
        float screenY = scoreboardModule.getScreenY();
        float width = scoreboardModule.getWidth();
        float height = scoreboardModule.getHeight();
        
        if (width <= 0 || height <= 0) {

            return;
        }
        

        boolean hovering = mouseX >= screenX && mouseX <= screenX + width && 
                          mouseY >= screenY && mouseY <= screenY + height;
        boolean shouldDrawBorder = hovering || element == draggingElement;
        
        if (shouldDrawBorder) {
            int borderColor = element == draggingElement ? Color.RED.getRGB() : Color.YELLOW.getRGB();
            

            RenderUtils.fill(event.getStack(), screenX, screenY, screenX + width, screenY + 1, borderColor);
            RenderUtils.fill(event.getStack(), screenX, screenY + height - 1, screenX + width, screenY + height, borderColor);
            RenderUtils.fill(event.getStack(), screenX, screenY, screenX + 1, screenY + height, borderColor);
            RenderUtils.fill(event.getStack(), screenX + width - 1, screenY, screenX + width, screenY + height, borderColor);
            

            font.render(event.getStack(), element.displayName,
                    screenX + 2, screenY - 12, Color.WHITE, true, 0.3);
        }
    }

    
    private void handleMouseInput(double mouseX, double mouseY) {

        boolean mousePressed = org.lwjgl.glfw.GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), 0) == org.lwjgl.glfw.GLFW.GLFW_PRESS;

        if (mousePressed && draggingElement == null) {
            startDragging(mouseX, mouseY);
        } else if (!mousePressed && draggingElement != null) {
            stopDragging();
        }
    }

    
    private void drawElementBorder(com.mojang.blaze3d.vertex.PoseStack poseStack, HUDElement element, int color) {

        RenderUtils.fill(poseStack, (float)element.x, (float)element.y, (float)(element.x + element.width), (float)(element.y + 1), color);
        RenderUtils.fill(poseStack, (float)element.x, (float)(element.y + element.height - 1), (float)(element.x + element.width), (float)(element.y + element.height), color);
        RenderUtils.fill(poseStack, (float)element.x, (float)element.y, (float)(element.x + 1), (float)(element.y + element.height), color);
        RenderUtils.fill(poseStack, (float)(element.x + element.width - 1), (float)element.y, (float)(element.x + element.width), (float)(element.y + element.height), color);
    }

    
    public HUDElement getHUDElement(String name) {
        return hudElements.get(name);
    }

    
    public java.util.Collection<HUDElement> getAllElements() {
        return hudElements.values();
    }

    
    public boolean isEditMode() {
        return editMode;
    }

    
    public static class HUDElement {
        public String name;
        public String displayName;
        public double x, y;
        public double width, height;

        public HUDElement(String name, String displayName, double x, double y, double width, double height) {
            this.name = name;
            this.displayName = displayName;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        
        public boolean isHovering(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}