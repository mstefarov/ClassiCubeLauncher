package net.classicube.launcher;

import java.io.File;

public class Paths {

    private static final String MacSuffix = "/Library/Application Support";
    public static final String ClientDirName = "net.classicube.client",
            ClientJar = "ClassiCubeClient.jar",
            ClientTempJar = "ClassiCubeClient.jar.tmp",
            LauncherDirName = "net.classicube.launcher",
            LogFileName = "launcher.log",
            LibsDirName = "libs";
            

    public static File getClientJar() {
        if (clientJar == null) {
            final File targetPath = getLauncherDir();
            clientJar = new File(targetPath, ClientJar);
        }
        return clientJar;
    }

    public static File getClientDir() {
        if(clientPath == null){
            clientPath = new File(getAppDataDir(), ClientDirName);
        }
        if (!clientPath.exists() && !clientPath.mkdirs()) {
            throw new RuntimeException("The working directory could not be created: " + clientPath);
        }
        return clientPath;
    }

    public static File getAppDataDir() {
        if (appDataPath == null) {
            final String home = System.getProperty("user.home", ".");
            final OperatingSystem os = getOs();

            switch (os) {
                case Windows:
                    String appData = System.getenv("APPDATA");
                    if (appData != null) {
                        appDataPath = new File(appData);
                    } else {
                        appDataPath = new File(home);
                    }
                    break;

                case MacOS:
                    appDataPath = new File(home, MacSuffix);
                    break;

                default:
                    appDataPath = new File(home);
            }
        }
        return appDataPath;
    }

    private static OperatingSystem getOs() {
        final String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return OperatingSystem.Windows;
        } else if (osName.contains("mac")) {
            return OperatingSystem.MacOS;
        } else if (osName.contains("solaris") || osName.contains("sunos")) {
            return OperatingSystem.Solaris;
        } else if (osName.contains("linux") || osName.contains("unix")) {
            return OperatingSystem.Nix;
        } else {
            return OperatingSystem.UNKNOWN;
        }
    }

    public static File getLauncherDir() {
        if (launcherPath == null) {
            final File userDir = getAppDataDir();
            launcherPath = new File(userDir, LauncherDirName);
            if (launcherPath.exists()) {
                launcherPath.mkdir();
            }
        }
        return launcherPath;
    }

    public static File getLogFile() {
        if (logFilePath == null) {
            logFilePath = new File(Paths.getLauncherDir(), Paths.LogFileName);
        }
        return logFilePath;
    }
    private static File clientJar,
            clientPath,
            launcherPath,
            logFilePath,
            appDataPath;
}
