package net.classicube.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

final class PathUtil {

    private static final String MAC_PATH_SUFFIX = "/Library/Application Support",
            CLIENT_DIR_NAME = ".net.classicube.client",
            LAUNCHER_DIR_NAME = ".net.classicube.launcher",
            LOG_FILE_NAME = "launcher.log";
    public static final String LZMA_JAR_NAME = "lzma.jar";

    public static File getClientDir() {
        if (clientPath == null) {
            clientPath = new File(getAppDataDir(), CLIENT_DIR_NAME);
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
                    appDataPath = new File(home, MAC_PATH_SUFFIX);
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
            launcherPath = new File(userDir, LAUNCHER_DIR_NAME);
            if (launcherPath.exists()) {
                launcherPath.mkdir();
            }
        }
        return launcherPath;
    }

    public static File getLogFile() {
        if (logFilePath == null) {
            logFilePath = new File(PathUtil.getLauncherDir(), PathUtil.LOG_FILE_NAME);
        }
        return logFilePath;
    }

    // Safely replace contents of destFile with sourceFile
    public static void replaceFile(final File sourceFile, final File destFile)
            throws IOException {
        if (sourceFile == null) {
            throw new NullPointerException("sourceFile");
        }
        if (destFile == null) {
            throw new NullPointerException("destFile");
        }
        Path sourcePath = Paths.get(sourceFile.getAbsolutePath());
        Path destPath = Paths.get(destFile.getAbsolutePath());
        try {
            Files.move(sourcePath, destPath, FileReplaceOptions);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(sourcePath, destPath, FallbackFileReplaceOptions);
        }
    }
    private static final CopyOption[] FileReplaceOptions = new CopyOption[]{
        StandardCopyOption.ATOMIC_MOVE,
        StandardCopyOption.REPLACE_EXISTING
    };
    private static final CopyOption[] FallbackFileReplaceOptions = new CopyOption[]{
        StandardCopyOption.REPLACE_EXISTING
    };

    // Deletes a directory and all of its children
    public boolean deleteDir(final File dir) {
        if (dir == null) {
            throw new NullPointerException("dir");
        }
        if (dir.isDirectory()) {
            final String[] files = dir.list();
            for (final String file : files) {
                final boolean success = deleteDir(new File(dir, file));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    //Copies a directory, creates dir if needed
    public static void copyDir(final File sourceDir, final File destDir)
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
            for (final String file : files) {
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

        try (final FileChannel source = new FileInputStream(sourceFile).getChannel()) {
            try (final FileChannel destination = new FileOutputStream(destFile).getChannel()) {
                destination.transferFrom(source, 0, source.size());
            }
        }
    }
    private static File clientPath,
            launcherPath,
            logFilePath,
            appDataPath;
}
