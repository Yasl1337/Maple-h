package com.heypixel.heypixelmod.obsoverlay.files.impl;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.exceptions.NoSuchModuleException;
import com.heypixel.heypixelmod.obsoverlay.files.ClientFile;
import com.heypixel.heypixelmod.obsoverlay.files.FileManager;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.KillSay;
import com.heypixel.heypixelmod.obsoverlay.values.HasValue;
import com.heypixel.heypixelmod.obsoverlay.values.Value;
import com.heypixel.heypixelmod.obsoverlay.values.ValueManager;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

public class ConfigFile extends ClientFile {
    private static final Logger logger = LogManager.getLogger(ConfigFile.class);

    public ConfigFile(String fileName) {
        super(fileName);
        this.file = new File(FileManager.configFolder, fileName);
    }

    @Override
    public void read(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) continue;

            try {
                if (line.startsWith("MODULE:")) {
                    readModuleLine(line.substring(7));
                } else if (line.startsWith("VALUE:")) {
                    readValueLine(line.substring(6));
                } else if (line.startsWith("KILLSAY:")) {
                    readKillSayLine(line.substring(8));
                } else {
                    logger.warn("Unknown line format in config file: {}", line);
                }
            } catch (Exception e) {
                logger.error("Failed to parse config line: {}", line, e);
            }
        }
    }

    private void readModuleLine(String line) throws NoSuchModuleException {
        String[] split = line.split(":", 3);
        if (split.length != 3) {
            logger.error("Failed to read module line: {}", line);
            return;
        }

        String name = split[0];
        int key = Integer.parseInt(split[1]);
        boolean enabled = Boolean.parseBoolean(split[2]);

        Module module = Naven.getInstance().getModuleManager().getModule(name);
        if (module != null) {
            // 优化：只有当状态不同时才更新
            if (module.isEnabled() != enabled) {
                module.setEnabled(enabled);
            }
            if (module.getKey() != key) {
                module.setKey(key);
            }
        } else {
            logger.warn("Failed to find module {}!", name);
        }
    }

    private void readValueLine(String line) {
        String[] split = line.split(":", 4);
        if (split.length != 4) {
            logger.error("Failed to read value line: {}", line);
            return;
        }

        String moduleName = split[0];
        String valueType = split[1];
        String valueName = split[2];
        String valueString = split[3];

        HasValue hasValue = Naven.getInstance().getHasValueManager().getHasValue(moduleName);
        if (hasValue == null) {
            logger.warn("HasValue not found for module: {}", moduleName);
            return;
        }

        Value value = Naven.getInstance().getValueManager().getValue(hasValue, valueName);
        if (value == null) {
            logger.warn("Value not found for module {} with name {}: {}", moduleName, valueName, valueString);
            return;
        }

        try {
            switch (valueType) {
                case "B":
                    boolean booleanValue = Boolean.parseBoolean(valueString);
                    if (value.getBooleanValue().getCurrentValue() != booleanValue) {
                        value.getBooleanValue().setCurrentValue(booleanValue);
                    }
                    break;
                case "F":
                    float floatValue = Float.parseFloat(valueString);
                    if (value.getFloatValue().getCurrentValue() != floatValue) {
                        value.getFloatValue().setCurrentValue(floatValue);
                    }
                    break;
                case "S":
                    String stringValue = valueString;
                    if (!value.getStringValue().getCurrentValue().equals(stringValue)) {
                        value.getStringValue().setCurrentValue(stringValue);
                    }
                    break;
                case "M":
                    int index = Integer.parseInt(valueString);
                    ModeValue modeValue = value.getModeValue();
                    if (modeValue.getCurrentValue() != index) {
                        if (index >= 0 && index < modeValue.getValues().length) {
                            modeValue.setCurrentValue(index);
                        } else {
                            logger.error("Failed to read mode value, index out of bounds: {}", line);
                        }
                    }
                    break;
                default:
                    logger.error("Unknown value type: {}", valueType);
            }
        } catch (Exception e) {
            logger.error("Failed to set value for line: {}", line, e);
        }
    }

    private void readKillSayLine(String line) {
        try {
            KillSay killSayModule = (KillSay)Naven.getInstance().getModuleManager().getModule(KillSay.class);
            if (killSayModule == null) {
                logger.warn("KillSay module not found.");
                return;
            }
            String[] split = line.split(":", 2);
            if (split.length != 2) {
                logger.error("Failed to read killsay line: {}", line);
                return;
            }
            String killSayName = split[0];
            boolean isEnabled = Boolean.parseBoolean(split[1]);

            for (BooleanValue value : killSayModule.getValues()) {
                // 优化：只有当状态不同时才更新
                if (value.getName().equals(killSayName) && value.getCurrentValue() != isEnabled) {
                    value.setCurrentValue(isEnabled);
                    return;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to process killsay line: {}", line, e);
        }
    }

    @Override
    public void save(BufferedWriter writer) throws IOException {
        // 保存模块启用状态和按键绑定
        for (Module module : Naven.getInstance().getModuleManager().getModules()) {
            writer.write(String.format("MODULE:%s:%d:%s\n", module.getName(), module.getKey(), module.isEnabled()));
        }

        // 保存所有模块的Value
        ValueManager valueManager = Naven.getInstance().getValueManager();
        for (Value value : valueManager.getValues()) {
            try {
                String moduleName = value.getKey().getName();
                String valueType;
                String valueString;

                switch (value.getValueType()) {
                    case BOOLEAN:
                        valueType = "B";
                        valueString = String.valueOf(value.getBooleanValue().getCurrentValue());
                        break;
                    case FLOAT:
                        valueType = "F";
                        valueString = String.valueOf(value.getFloatValue().getCurrentValue());
                        break;
                    case STRING:
                        valueType = "S";
                        valueString = value.getStringValue().getCurrentValue();
                        break;
                    case MODE:
                        valueType = "M";
                        valueString = String.valueOf(value.getModeValue().getCurrentValue());
                        break;
                    default:
                        logger.error("Unknown value type for value {}!", value.getKey().getName());
                        continue;
                }

                writer.write(String.format("VALUE:%s:%s:%s:%s\n", moduleName, valueType, value.getName(), valueString));
            } catch (Exception e) {
                logger.error("Failed to save value {}!", value.getKey().getName(), e);
            }
        }

        // 保存 KillSay 设置
        KillSay killSayModule = (KillSay)Naven.getInstance().getModuleManager().getModule(KillSay.class);
        if (killSayModule != null) {
            for (BooleanValue killSay : killSayModule.getValues()) {
                writer.write(String.format("KILLSAY:%s:%s\n", killSay.getName(), killSay.getCurrentValue()));
            }
        }
    }
}