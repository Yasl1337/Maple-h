package com.heypixel.heypixelmod.obsoverlay.commands.impl;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.commands.Command;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandInfo;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventKey;
import com.heypixel.heypixelmod.obsoverlay.exceptions.NoSuchModuleException;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;

@CommandInfo(
   name = "bind",
   description = "Bind a command to a key",
   aliases = {"b"}
)
public class CommandBind extends Command {
   @Override
   public void onCommand(String[] args) {
      if (args.length == 1) {
         final String moduleName = args[0];

         try {
            final Module module = Naven.getInstance().getModuleManager().getModule(moduleName);
            if (module != null) {
               ChatUtils.addChatMessage("Press a key to bind " + moduleName + " to.");
               Naven.getInstance().getEventManager().register(new Object() {
                  @EventTarget
                  public void onKey(EventKey e) {
                     if (e.isState()) {
                        module.setKey(e.getKey());
                        Key key = InputConstants.getKey(e.getKey(), 0);
                        String keyName = key.getDisplayName().getString().toUpperCase();
                        ChatUtils.addChatMessage("Bound " + moduleName + " to " + keyName + ".");
                        Naven.getInstance().getEventManager().unregister(this);
                        Naven.getInstance().getFileManager().save();
                     }
                  }
               });
            } else {
               ChatUtils.addChatMessage("Invalid module.");
            }
         } catch (NoSuchModuleException var7) {
            ChatUtils.addChatMessage("Invalid module.");
         }
      } else if (args.length == 2) {
         String moduleName = args[0];
         String keyName = args[1];

         try {
            Module module = Naven.getInstance().getModuleManager().getModule(moduleName);
            if (module != null) {
               if (keyName.equalsIgnoreCase("none")) {
                  module.setKey(InputConstants.UNKNOWN.getValue());
                  ChatUtils.addChatMessage("Unbound " + moduleName + ".");
                  Naven.getInstance().getFileManager().save();
               } else {
                  // 特殊处理RShift键
                  if (keyName.equalsIgnoreCase("rshift")) {
                     module.setKey(345); // GLFW_KEY_RIGHT_SHIFT的值
                     ChatUtils.addChatMessage("Bound " + moduleName + " to RSHIFT.");
                     Naven.getInstance().getFileManager().save();
                     return;
                  }
                  
                  Key key = InputConstants.getKey("key.keyboard." + keyName.toLowerCase());
                  if (key != InputConstants.UNKNOWN) {
                     module.setKey(key.getValue());
                     ChatUtils.addChatMessage("Bound " + moduleName + " to " + keyName.toUpperCase() + ".");
                     Naven.getInstance().getFileManager().save();
                  } else {
                     ChatUtils.addChatMessage("Invalid key.");
                  }
               }
            } else {
               ChatUtils.addChatMessage("Invalid module.");
            }
         } catch (NoSuchModuleException var6) {
            ChatUtils.addChatMessage("Invalid module.");
         }
      } else {
         ChatUtils.addChatMessage("Usage: .bind <module> [key]");
      }
   }

   @Override
   public String[] onTab(String[] args) {
      // 添加RShift到tab补全建议中
      if (args.length == 2) {
         String[] baseSuggestions = Naven.getInstance()
            .getModuleManager()
            .getModules()
            .stream()
            .map(Module::getName)
            .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
            .toArray(String[]::new);
         
         // 如果正在输入键名，提供常用键的建议，包括RShift
         String[] keySuggestions = {"rshift", "none", "a", "b", "c", "shift", "ctrl", "alt"};
         if (args[1].isEmpty() || "r".startsWith(args[1].toLowerCase())) {
             return keySuggestions;
         }
         return java.util.Arrays.stream(keySuggestions)
             .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
             .toArray(String[]::new);
      }
      
      return Naven.getInstance()
         .getModuleManager()
         .getModules()
         .stream()
         .map(Module::getName)
         .filter(name -> name.toLowerCase().startsWith(args.length == 0 ? "" : args[0].toLowerCase()))
         .toArray(String[]::new);
   }
}