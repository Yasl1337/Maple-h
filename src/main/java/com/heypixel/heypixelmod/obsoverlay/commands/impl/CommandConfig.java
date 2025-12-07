package com.heypixel.heypixelmod.obsoverlay.commands.impl;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.commands.Command;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandInfo;
import com.heypixel.heypixelmod.obsoverlay.files.FileManager;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@CommandInfo(
        name = "config",
        description = "Manages client configurations.",
        aliases = {"cfg"}
)
public class CommandConfig extends Command {

   @Override
   public void onCommand(String[] args) {
      if (args.length == 0) {
         ChatUtils.addChatMessage("§7=======§6Config System§7=======");
         ChatUtils.addChatMessage("§7Note: The config name does not include the .cfg suffix.");
         ChatUtils.addChatMessage("§7Parent command: .config / .cfg");
         ChatUtils.addChatMessage("§7.config open : Opens the config folder");
         ChatUtils.addChatMessage("§7.config list  : Lists available configs");
         ChatUtils.addChatMessage("§7.config load <config_name> : Loads a config");
         ChatUtils.addChatMessage("§7.config save <config_name> : Saves a config");
         return;
      }

      String subCommand = args[0].toLowerCase();

      switch (subCommand) {
         case "open":
            try {
               Runtime.getRuntime().exec("explorer " + FileManager.configFolder.getAbsolutePath());
            } catch (IOException var3) {
               ChatUtils.addChatMessage("§cCould not open the config folder.");
            }
            break;
         case "load":
            if (args.length < 2) {
               ChatUtils.addChatMessage("§cUsage: .config load <config_name>");
               return;
            }
            Naven.getInstance().getFileManager().load(args[1]);
            break;
         case "save":
            if (args.length < 2) {
               ChatUtils.addChatMessage("§cUsage: .config save <config_name>");
               return;
            }
            Naven.getInstance().getFileManager().save(args[1]);
            break;
         case "list":
            ChatUtils.addChatMessage("§7--- §6Available Configs §7---");
            try (Stream<Path> paths = Files.list(FileManager.configFolder.toPath())) {
               paths.filter(Files::isRegularFile)
                       .map(Path::getFileName)
                       .map(Path::toString)
                       .filter(name -> name.endsWith(".cfg"))
                       .forEach(name -> ChatUtils.addChatMessage("§7- §b" + name.replace(".cfg", "")));
            } catch (IOException e) {
               ChatUtils.addChatMessage("§cCould not read the config file list.");
            }
            ChatUtils.addChatMessage("§7--------------------");
            break;
         default:
            ChatUtils.addChatMessage("§cUnknown command: " + subCommand + ", use .config to see the command list.");
            break;
      }
   }

   @Override
   public String[] onTab(String[] args) {
      if (args.length == 1) {
         return new String[]{"open", "load", "save", "list"};
      }
      return new String[0];
   }
}