package com.heypixel.heypixelmod.obsoverlay.files.impl;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.files.ClientFile;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Spammer;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class SpammerFile extends ClientFile {
   private static final String[] styles = new String[]{
      "我是PMCP使用者，你不可能战胜我！"
   };

   public SpammerFile() {
      super("spammers.cfg");
   }

   @Override
   public void read(BufferedReader reader) throws IOException {
      Spammer module = (Spammer)Naven.getInstance().getModuleManager().getModule(Spammer.class);
      List<BooleanValue> values = module.getValues();

      String line;
      while ((line = reader.readLine()) != null) {
         values.add(ValueBuilder.create(module, line).setDefaultBooleanValue(false).build().getBooleanValue());
      }

      if (values.isEmpty()) {
         for (String style : styles) {
            values.add(ValueBuilder.create(module, style).setDefaultBooleanValue(false).build().getBooleanValue());
         }
      }
   }

   @Override
   public void save(BufferedWriter writer) throws IOException {
      Spammer module = (Spammer)Naven.getInstance().getModuleManager().getModule(Spammer.class);

      for (BooleanValue value : module.getValues()) {
         writer.write(value.getName() + "\n");
      }
   }
}
