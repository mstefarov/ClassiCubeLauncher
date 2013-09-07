package net.classicube.launcher;

enum OperatingSystem {

    Nix(0), Solaris(1), Windows(2), MacOS(3), UNKNOWN(4);

    private OperatingSystem(int id) {
        this.id = id;
    }
    public final int id;
    public static final OperatingSystem[] values = new OperatingSystem[]{Nix, Solaris, Windows, MacOS, UNKNOWN};
}