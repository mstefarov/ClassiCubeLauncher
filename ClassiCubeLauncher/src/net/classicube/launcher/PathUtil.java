package net.classicube.launcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

final class PathUtil {

    public static final String CLIENT_DIR_NAME = ".net.classicube.client",
            LOG_FILE_NAME = "launcher.log",
            LOG_OLD_FILE_NAME = "launcher.old.log",
            CLIENT_LOG_FILE_NAME = "client.log",
            CLIENT_LOG_OLD_FILE_NAME = "client.old.log",
            OPTIONS_FILE_NAME = "options.txt",
            SELF_UPDATER_LOG_FILE_NAME = "selfupdater.log";
    private static File clientPath;

    // Find client's directory. If it does not exist, create it.
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

    public static void copyStreamToFile(InputStream inStream, File file) throws IOException {
        try (ReadableByteChannel in = Channels.newChannel(inStream)) {
            try (FileOutputStream outStream = new FileOutputStream(file)) {
                FileChannel out = outStream.getChannel();
                long offset = 0;
                long quantum = 1024 * 1024;
                long count;
                while ((count = out.transferFrom(in, offset, quantum)) > 0) {
                    offset += count;
                }
            }
        }
    }
}
