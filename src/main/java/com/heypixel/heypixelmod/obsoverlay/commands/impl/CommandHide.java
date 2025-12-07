package com.heypixel.heypixelmod.obsoverlay.commands.impl;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.commands.Command;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandInfo;
import com.heypixel.heypixelmod.obsoverlay.exceptions.NoSuchModuleException;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;

import java.util.List;
import java.util.stream.Collectors;

@CommandInfo(
        name = "hide",
        description = "Hide or unhide a module from the array list",
        aliases = {"h"}
)
public class CommandHide extends Command {

    @Override
    public void onCommand(String[] args) {
        if (args.length != 1) {
            ChatUtils.addChatMessage("Usage: .hide <module>");
            return;
        }

        String moduleName = args[0];
        try {
            Module module = Naven.getInstance().getModuleManager().getModule(moduleName);
            if (module != null) {
                module.setHidden(!module.isHidden());
                if (module.isHidden()) {
                    ChatUtils.addChatMessage("Hidden module " + module.getName() + ".");
                } else {
                    ChatUtils.addChatMessage("Unhidden module " + module.getName() + ".");
                }
                Naven.getInstance().getFileManager().save();
                Module.update = true;
            } else {
                ChatUtils.addChatMessage("Invalid module.");
            }
        } catch (NoSuchModuleException e) {
            ChatUtils.addChatMessage("Invalid module.");
        }
    }

    @Override
    public String[] onTab(String[] args) {
        List<String> moduleNames = Naven.getInstance().getModuleManager().getModules().stream()
                .map(Module::getName)
                .filter(name -> args.length == 0 || name.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());

        return moduleNames.toArray(new String[0]);
    }
}