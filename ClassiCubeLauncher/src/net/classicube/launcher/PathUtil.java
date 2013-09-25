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

    // Safely replace contents of destFile with sourceFile
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
            if (!destDir.exists() && !destDir.mkdirs()) {
                throw new IOException("Unable to create directory " + destDir);
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
        destFile.createNewFile();

        try (final FileChannel source = new FileInputStream(sourceFile).getChannel()) {
            try (final FileChannel destination = new FileOutputStream(destFile).getChannel()) {
                destination.transferFrom(source, 0, source.size());
            }
        }
    }
}
