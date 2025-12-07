package com.heypixel.heypixelmod.obsoverlay;

/**
 * 直接使用NavenUser.getUsername()调用即可
 **/
public class NavenUser {
    private static final String USERNAME = "Yas1nb";

    public static void ensureUserLoaded() {
        // 不再需要加载文件或显示登录屏幕
    }

    public static String getUsername() {
        return USERNAME;
    }
}