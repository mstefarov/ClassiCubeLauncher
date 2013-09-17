package net.classicube.selfupdater;

enum OperatingSystem {
    NIX,
    SOLARIS,
    WINDOWS,
    MACOS,
    UNKNOWN;
    
    private final static String osName = System.getProperty("os.name").toLowerCase();

    public static OperatingSystem detect() {
        if (osName.contains("win")) {
            return OperatingSystem.WINDOWS;
        } else if (osName.contains("mac")) {
            return OperatingSystem.MACOS;
        } else if (osName.contains("solaris") || osName.contains("sunos")) {
            return OperatingSystem.SOLARIS;
        } else if (osName.contains("linux") || osName.contains("unix")) {
            return OperatingSystem.NIX;
        } else {
            return OperatingSystem.UNKNOWN;
        }
    }
}