package net.classicube.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

final class PathUtil {

    private static final String MacSuffix = "/Library/Application Support";
    public static final String ClientDirName = "net.classicube.client",
            ClientJar = "ClassiCubeClient.jar",
            ClientTempJar = "ClassiCubeClient.jar.tmp",
            LauncherDirName = "net.classicube.launcher",
            LogFileName = "launcher.log",
            LibsDirName = "libs";

    public static File getClientDir() {
        if (clientPath == null) {
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
            final OperatingSystem os = OperatingSystem.detect();

            switch (os) {
                case WINDOWS:
                    final String appData = System.getenv("APPDATA");
                    if (appData != null) {
                        appDataPath = new File(appData);
                    } else {
                        appDataPath = new File(home);
                    }
                    break;

                case MACOS:
                    appDataPath = new File(home, MacSuffix);
                    break;

                default:
                    appDataPath = new File(home);
            }
        }
        return appDataPath;
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
            logFilePath = new File(PathUtil.getLauncherDir(), PathUtil.LogFileName);
        }
        return logFilePath;
    }

    // Safely replace contents of destFile with sourceFile
    public static void replaceFile(File sourceFile, File destFile)
            throws IOException {
        if (sourceFile == null) {
            throw new NullPointerException("sourceFile");
        }
        if (destFile == null) {
            throw new NullPointerException("destFile");
        }
        Path sourcePath = Paths.get(sourceFile.getAbsolutePath());
        Path destPath = Paths.get(destFile.getAbsolutePath());
        Files.move(sourcePath, destPath, FileReplaceOptions);
    }
    private static final CopyOption[] FileReplaceOptions = new CopyOption[]{
        StandardCopyOption.ATOMIC_MOVE,
        StandardCopyOption.REPLACE_EXISTING
    };

    // Deletes a directory and all of its children
    public boolean deleteDir(File dir) {
        if (dir == null) {
            throw new NullPointerException("dir");
        }
        if (dir.isDirectory()) {
            final String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                final boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    //Copies a directory, creates dir if needed
    public static void copyDir(File sourceDir, File destDir)
            throws FileNotFoundException, IOException {
        if (sourceDir == null) {
            throw new NullPointerException("sourceDir");
        }
        if (destDir == null) {
            throw new NullPointerException("destDir");
        }
        if (sourceDir.isDirectory()) {
            if (!destDir.exists()) {
                destDir.mkdir();
            }
            final String files[] = sourceDir.list();
            for (String file : files) {
                final File srcFile = new File(sourceDir, file);
                final File destFile = new File(destDir, file);
                copyDir(srcFile, destFile);
            }
        } else {
            copyFile(sourceDir, destDir);
        }
    }

    // Copies a file
    public static void copyFile(File sourceFile, File destFile)
            throws FileNotFoundException, IOException {
        if (sourceFile == null) {
            throw new NullPointerException("sourceFile");
        }
        if (destFile == null) {
            throw new NullPointerException("destFile");
        }
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        try (FileChannel source = new FileInputStream(sourceFile).getChannel()) {
            try (FileChannel destination = new FileOutputStream(destFile).getChannel()) {
                destination.transferFrom(source, 0, source.size());
            }
        }
    }

    private static File clientPath,
            launcherPath,
            logFilePath,
            appDataPath;
}
