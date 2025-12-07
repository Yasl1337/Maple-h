package com.heypixel.heypixelmod.obsoverlay.files.impl;

import com.heypixel.heypixelmod.obsoverlay.files.ClientFile;
import com.heypixel.heypixelmod.obsoverlay.ui.HUDEditor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public class HUDPositionFile extends ClientFile {
    private static final Logger logger = LogManager.getLogger(HUDPositionFile.class);

    public HUDPositionFile() {
        super("hud_positions.cfg");
    }

    @Override
    public void read(BufferedReader reader) throws IOException {
        HUDEditor hudEditor = HUDEditor.getInstance();
        
        String line;
        while ((line = reader.readLine()) != null) {
            String[] split = line.split(":", 5);
            if (split.length != 5) {
                logger.error("Failed to read HUD position line {}! Expected 5 parts.", line);
                continue;
            }
            
            try {
                String name = split[0];
                double x = Double.parseDouble(split[1]);
                double y = Double.parseDouble(split[2]);
                double width = Double.parseDouble(split[3]);
                double height = Double.parseDouble(split[4]);
                
                HUDEditor.HUDElement element = hudEditor.getHUDElement(name);
                if (element != null) {
                    element.x = x;
                    element.y = y;
                    element.width = width;
                    element.height = height;
                } else {
                    logger.warn("Unknown HUD element: {}", name);
                }
            } catch (NumberFormatException e) {
                logger.error("Failed to parse HUD position for line: {}", line, e);
            }
        }
    }

    @Override
    public void save(BufferedWriter writer) throws IOException {
        HUDEditor hudEditor = HUDEditor.getInstance();
        
        for (HUDEditor.HUDElement element : hudEditor.getAllElements()) {
            writer.write(String.format("%s:%.2f:%.2f:%.2f:%.2f%n", 
                element.name, element.x, element.y, element.width, element.height));
        }
    }
}
