package net.classicube.launcher;

import java.util.prefs.Preferences;

public final class Prefs {

    // Key names
    private static final String keyUpdateMode = "UpdateMode",
            keyFullscreen = "WindowSize",
            keyRememberUsers = "RememberUsers",
            keyRememberPasswords = "RememberPasswords",
            keyRememberServer = "RememberServer",
            keyJavaArgs = "JavaArgs",
            keyMaxMemory = "MaxMemory",
            keySelectedGameService = "SelectedGameService",
            keyDebugMode = "DebugMode",
            keyRememberedExternalIPs = "RememberedExternalIPs",
            keyKeepOpen = "KeepOpen";

    // Defaults
    public final static UpdateMode UpdateModeDefault = UpdateMode.NOTIFY;
    public final static boolean FullscreenDefault = false,
            RememberUsersDefault = true,
            RememberPasswordsDefault = true,
            RememberServerDefault = true,
            DebugModeDefault = false,
            KeepOpenDefault = false;
    public final static String JavaArgsDefault = "-Dorg.lwjgl.util.Debug=true "
            + "-Dsun.java2d.noddraw=true "
            + "-Dsun.awt.noerasebackground=true "
            + "-Dsun.java2d.d3d=false "
            + "-Dsun.java2d.opengl=false "
            + "-Dsun.java2d.pmoffscreen=false";
    public final static int MaxMemoryDefault = 800;
    public final static GameServiceType SelectedGameServiceDefault = GameServiceType.ClassiCubeNetService;

    // Getters
    public static UpdateMode getUpdateMode() {
        try {
            return UpdateMode.valueOf(getPrefs().get(keyUpdateMode, UpdateModeDefault.name()));
        } catch (final IllegalArgumentException ex) {
            return UpdateModeDefault;
        }
    }

    public static boolean getFullscreen() {
        return getPrefs().getBoolean(keyFullscreen, FullscreenDefault);
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

    public static boolean getDebugMode() {
        return getPrefs().getBoolean(keyDebugMode, DebugModeDefault);
    }

    public static GameServiceType getSelectedGameService() {
        try {
            final String val = getPrefs().get(keySelectedGameService, SelectedGameServiceDefault.name());
            return GameServiceType.valueOf(val);
        } catch (final IllegalArgumentException ex) {
            return SelectedGameServiceDefault;
        }
    }
    
    public static boolean getKeepOpen() {
        return getPrefs().getBoolean(keyKeepOpen, KeepOpenDefault);
    }

    // Setters
    public static void setUpdateMode(final UpdateMode val) {
        getPrefs().put(keyUpdateMode, val.name());
    }

    public static void setFullscreen(final boolean val) {
        getPrefs().putBoolean(keyFullscreen, val);
    }

    public static void setRememberUsers(final boolean val) {
        getPrefs().putBoolean(keyRememberUsers, val);
    }

    public static void setRememberPasswords(final boolean val) {
        getPrefs().putBoolean(keyRememberPasswords, val);
    }

    public static void setRememberServer(final boolean val) {
        getPrefs().putBoolean(keyRememberServer, val);
    }

    public static void setJavaArgs(final String val) {
        getPrefs().put(keyJavaArgs, val);
    }

    public static void setMaxMemory(final int val) {
        getPrefs().putInt(keyMaxMemory, val);
    }

    public static void setDebugMode(final boolean val) {
        getPrefs().putBoolean(keyDebugMode, val);
    }

    public static void setSelectedGameService(final GameServiceType val) {
        getPrefs().put(keySelectedGameService, val.name());
    }
    
    public static void setKeepOpen(final boolean val) {
        getPrefs().putBoolean(keyKeepOpen, val);
    }

    // Etc
    private static Preferences getPrefs() {
        return Preferences.userNodeForPackage(Prefs.class);
    }
    
    public static Preferences getRememberedExternalIPs() {
        return getPrefs().node(keyRememberedExternalIPs);
    }

    private Prefs() {
    }
}
