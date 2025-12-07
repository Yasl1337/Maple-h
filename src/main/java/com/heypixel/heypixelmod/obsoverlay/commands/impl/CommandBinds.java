package com.heypixel.heypixelmod.obsoverlay.commands.impl;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.commands.Command;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.List;

@CommandInfo(
        name = "binds",
        description = "Displays all modules with a bound key.",
        aliases = {"bds"}
)
public class CommandBinds extends Command {

    @Override
    public void onCommand(String[] args) {
        List<Module> modules = Naven.getInstance().getModuleManager().getModules();

        ChatUtils.addChatMessage("--- Active Binds ---");

        boolean foundBinds = false;

        for (Module module : modules) {
            int key = module.getKey();

            if (key != InputConstants.UNKNOWN.getValue()) {
                String moduleName = module.getName();
                String keyName = InputConstants.getKey(key, 0).getDisplayName().getString().toUpperCase();

                if (!"KEY.KEYBOARD.0".equalsIgnoreCase(keyName)) {
                    ChatUtils.addChatMessage(String.format("%s: %s", moduleName, keyName));
                    foundBinds = true;
                }
            }
        }

        if (!foundBinds) {
            ChatUtils.addChatMessage("No active binds found.");
        }

        ChatUtils.addChatMessage("--------------------");
    }

    @Override
    public String[] onTab(String[] args) {
        return new String[0];
    }
}