package net.classicube.launcher;

enum OperatingSystem {

    Nix(0), Solaris(1), Windows(2), MacOS(3), Unknown(4);
    private final static String osName = System.getProperty("os.name").toLowerCase();

    private OperatingSystem(int id) {
        this.id = id;
    }

    public static OperatingSystem detect() {
        if (osName.contains("win")) {
            return OperatingSystem.Windows;
        } else if (osName.contains("mac")) {
            return OperatingSystem.MacOS;
        } else if (osName.contains("solaris") || osName.contains("sunos")) {
            return OperatingSystem.Solaris;
        } else if (osName.contains("linux") || osName.contains("unix")) {
            return OperatingSystem.Nix;
        } else {
            return OperatingSystem.Unknown;
        }
    }
    public final int id;
}