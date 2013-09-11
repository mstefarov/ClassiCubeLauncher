package net.classicube.launcher;

import java.util.prefs.Preferences;

final class Prefs {
    // Key names

    private static String keyUpdateMode = "UpdateMode",
            keyWindowSize = "WindowSize",
            keyRememberUsers = "RememberUsers",
            keyRememberPasswords = "RememberPasswords",
            keyRememberServer = "RememberServer",
            keyJavaArgs = "JavaArgs",
            keyMaxMemory = "MaxMemory",
            keySelectedGameService = "SelectedGameService";
    // Defaults
    public static UpdateMode UpdateModeDefault = UpdateMode.NOTIFY;
    public static WindowSize WindowSizeDefault = WindowSize.NORMAL;
    public static boolean RememberUsersDefault = true,
            RememberPasswordsDefault = true,
            RememberServerDefault = true;
    public static String JavaArgsDefault = "-Dorg.lwjgl.util.Debug=true "
            + "-Dsun.java2d.noddraw=true "
            + "-Dsun.awt.noerasebackground=true "
            + "-Dsun.java2d.d3d=false "
            + "-Dsun.java2d.opengl=false "
            + "-Dsun.java2d.pmoffscreen=false";
    public static int MaxMemoryDefault = 800;
    public static GameServiceType SelectedGameServiceDefault = GameServiceType.ClassiCubeNetService;

    // Getters
    public static UpdateMode getUpdateMode() {
        try {
            return UpdateMode.valueOf(getPrefs().get(keyUpdateMode, UpdateModeDefault.name()));
        } catch (IllegalArgumentException ex) {
            return UpdateModeDefault;
        }
    }

    public static WindowSize getWindowSize() {
        try {
            return WindowSize.valueOf(getPrefs().get(keyWindowSize, WindowSizeDefault.name()));
        } catch (IllegalArgumentException ex) {
            return WindowSizeDefault;
        }
    }

    public static boolean getRememberUsers() {
        return getPrefs().getBoolean(keyRememberUsers, RememberUsersDefault);
    }

    public static boolean getRememberPasswords() {
        return getPrefs().getBoolean(keyRememberPasswords, RememberPasswordsDefault);
    }

    public static boolean getRememberServer() {
        return getPrefs().getBoolean(keyRememberServer, RememberServerDefault);
    }

    public static String getJavaArgs() {
        return getPrefs().get(keyJavaArgs, JavaArgsDefault);
    }

    public static int getMaxMemory() {
        return getPrefs().getInt(keyMaxMemory, MaxMemoryDefault);
    }

    public static GameServiceType getSelectedGameService() {
        try {
            String val = getPrefs().get(keySelectedGameService, SelectedGameServiceDefault.name());
            return GameServiceType.valueOf(val);
        } catch (IllegalArgumentException ex) {
            return SelectedGameServiceDefault;
        }
    }

    // Setters
    public static void setUpdateMode(UpdateMode val) {
        getPrefs().put(keyUpdateMode, val.name());
    }

    public static void setWindowSize(WindowSize val) {
        getPrefs().put(keyWindowSize, val.name());
    }

    public static void setRememberUsers(boolean val) {
        getPrefs().putBoolean(keyRememberUsers, val);
    }

    public static void setRememberPasswords(boolean val) {
        getPrefs().putBoolean(keyRememberPasswords, val);
    }

    public static void setRememberServer(boolean val) {
        getPrefs().putBoolean(keyRememberServer, val);
    }

    public static void setJavaArgs(String val) {
        getPrefs().put(keyJavaArgs, val);
    }

    public static void setMaxMemory(int val) {
        getPrefs().putInt(keyMaxMemory, val);
    }

    public static void setSelectedGameService(GameServiceType val) {
        getPrefs().put(keySelectedGameService, val.name());
    }

    // Etc
    private static Preferences getPrefs() {
        return Preferences.userNodeForPackage(Prefs.class);
    }
}
