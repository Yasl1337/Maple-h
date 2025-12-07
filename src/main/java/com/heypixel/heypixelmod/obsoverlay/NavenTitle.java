package com.heypixel.heypixelmod.obsoverlay;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.heypixel.heypixelmod.obsoverlay.Naven.CLIENT_NAME;
import static com.heypixel.heypixelmod.obsoverlay.modules.Module.mc;
public class NavenTitle {
    public String getTitle() {
        final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        String Title = CLIENT_NAME + " | " + Version.getVersion() + " | " + StringUtils.split(mc.fpsString, " ")[0] + " FPS | " + format.format(new Date());
        return Title;
    }
}