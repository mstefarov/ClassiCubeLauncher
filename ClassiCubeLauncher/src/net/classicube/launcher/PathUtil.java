package net.classicube.launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

final class PathUtil {

    public static final String CLIENT_DIR_NAME = ".net.classicube.client",
            LOG_FILE_NAME = "launcher.log";
    private static File clientPath;

    public synchronized static File getClientDir() {
        if (clientPath == null) {
            clientPath = new File(SharedUpdaterCode.getAppDataDir(), CLIENT_DIR_NAME);
        }
        if (!clientPath.exists() && !clientPath.mkdirs()) {
            throw new RuntimeException("The working directory could not be created: " + clientPath);
        }
        return clientPath;
    }

    public static File getJavaPath() {
        return new File(System.getProperty("java.home"), "bin/java");
    }

    // Safely replace contents of destFile with sourceFile.
    public synchronized static void replaceFile(final File sourceFile, final File destFile)
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
}
