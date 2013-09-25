package net.classicube.launcher;

import net.classicube.launcher.gui.ClientUpdateScreen;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.SwingWorker;

public final class ClientUpdateTask
        extends SwingWorker<Boolean, ClientUpdateTask.ProgressUpdate> {

    // =============================================================================================
    //                                                                    CONSTANTS & INITIALIZATION
    // =============================================================================================
    private static final ClientUpdateTask instance = new ClientUpdateTask();
    private boolean updatesApplied;

    public static ClientUpdateTask getInstance() {
        return instance;
    }

    private ClientUpdateTask() {
    }

    // =============================================================================================
    //                                                                                          MAIN
    // =============================================================================================
    @Override
    protected Boolean doInBackground()
            throws Exception {
        if (Prefs.getUpdateMode() == UpdateMode.DISABLED) {
            return true;
        }

        this.digest = MessageDigest.getInstance("SHA1");
        Logger logger = LogUtil.getLogger();

        // step 1: build up file list
        logger.log(Level.INFO, "Checking for updates.");
        List<FileToDownload> files = pickFilesToDownload();

        if (files.isEmpty()) {
            logger.log(Level.INFO, "No updates needed.");

        } else {
            this.updatesApplied = true;
            logger.log(Level.INFO, "Downloading updates: {0}", listFileNames(files));

            this.activeFileNumber = 0;
            this.totalFiles = files.size();
            for (final FileToDownload file : files) {
                this.activeFile = file;
                try {
                    // step 2: download
                    signalDownloadProgress();
                    final File downloadedFile = downloadFile(file);

                    // step 3: unpack
                    signalUnpackProgress();
                    final File processedFile = SharedUpdaterCode.processDownload(
                            LogUtil.getLogger(),
                            downloadedFile, file.remoteUrl, file.localName.getName());

                    // step 4: deploy
                    deployFile(processedFile, file.localName);

                } catch (final IOException ex) {
                    logger.log(Level.SEVERE, "Error downloading an updated file.", ex);
                }
                this.activeFileNumber++;
            }
            logger.log(Level.INFO, "Updates applied.");
        }

        verifyFiles(files);

        return true;
    }

    private List<FileToDownload> findFilesToDownload() throws IOException {
        final List<FileToDownload> files = new ArrayList<>();

        final File clientDir = PathUtil.getClientDir();
        final File launcherDir = SharedUpdaterCode.getLauncherDir();

        files.add(new FileToDownload(
                "lzma.jar",
                new File(launcherDir, "lzma.jar")));

        files.add(new FileToDownload(
                "launcher.jar.pack.lzma",
                new File(launcherDir, SharedUpdaterCode.LAUNCHER_NEW_JAR_NAME)));

        files.add(new FileToDownload(
                "client.jar.pack.lzma",
                new File(clientDir, "client.jar")));

        files.add(new FileToDownload(
                "lwjgl.jar.pack.lzma",
                new File(clientDir, "libs/lwjgl.jar")));
        files.add(new FileToDownload(
                "lwjgl_util.jar.pack.lzma",
                new File(clientDir, "libs/lwjgl_util.jar")));
        files.add(new FileToDownload(
                "jinput.jar.pack.lzma",
                new File(clientDir, "libs/jinput.jar")));
        files.add(pickNativeDownload());

        return files;
    }

    private static String listFileNames(final List<FileToDownload> files) {
        final StringBuilder sb = new StringBuilder();
        String sep = "";
        for (final FileToDownload s : files) {
            sb.append(sep).append(s.localName.getName());
            sep = ", ";
        }
        return sb.toString();
    }
    // =============================================================================================
    //                                                                        CHECKING / DOWNLOADING
    // =============================================================================================
    private MessageDigest digest;
    private final byte[] ioBuffer = new byte[64 * 1024];

    private List<FileToDownload> pickFilesToDownload() throws IOException {
        List<FileToDownload> filesToDownload = new ArrayList<>();
        List<FileToDownload> localFiles = findFilesToDownload();
        HashMap<String, RemoteFile> remoteFiles = getRemoteIndex();

        for (FileToDownload localFile : localFiles) {
            signalCheckProgress(localFile.localName.getName());
            RemoteFile remoteFile = remoteFiles.get(localFile.remoteUrl);
            boolean isLzma = localFile.localName.getName().equals(SharedUpdaterCode.LZMA_JAR_NAME);
            boolean download = false;
            if (!localFile.localName.exists()) {
                // If local file does not exist
                if (remoteFile != null) {
                    LogUtil.getLogger().log(Level.INFO,
                            "ClientUpdateTask: Will download {0}: does not exist locally",
                            localFile.localName.getName());
                    download = true;
                } else {
                    throw new RuntimeException("Required file \"" + localFile.remoteUrl + "\" does not exist.");
                }
            } else if (!isLzma) {
                if (remoteFile != null) {
                    try {
                        String localHash = computeLocalHash(localFile.localName);
                        if (!localHash.equalsIgnoreCase(remoteFile.hash)) {
                            // If file contents don't match
                            LogUtil.getLogger().log(Level.INFO,
                                    "Will download {0}: contents don''t match ({1} vs {2})",
                                    new Object[]{localFile.localName.getName(), localHash, remoteFile.hash});
                            download = true;
                        }
                    } catch (IOException ex) {
                        LogUtil.getLogger().log(Level.SEVERE,
                                "Error computing hash of a local file", ex);
                    }
                } else {
                    LogUtil.getLogger().log(Level.WARNING,
                            "No remote match for local file {0}", localFile.localName.getName());
                }
            }
            if (download) {
                localFile.remoteLength = remoteFile.length;
                filesToDownload.add(localFile);
            }
        }
        return filesToDownload;
    }

    private HashMap<String, RemoteFile> getRemoteIndex() {
        String hashIndex = HttpUtil.downloadString("http://www.classicube.net/static/client/version");
        HashMap<String, RemoteFile> remoteFiles = new HashMap<>();

        // special treatment for LZMA
        RemoteFile lzmaFile = new RemoteFile();
        lzmaFile.name = SharedUpdaterCode.LZMA_JAR_NAME;
        lzmaFile.length = 7187;
        lzmaFile.hash = "N/A";
        remoteFiles.put(lzmaFile.name.toLowerCase(), lzmaFile);

        // the rest of the files
        for (String line : hashIndex.split("\\r?\\n")) {
            String[] components = line.split(" ");
            RemoteFile file = new RemoteFile();
            file.name = components[0];
            file.length = Long.parseLong(components[1]);
            file.hash = components[2].toLowerCase();
            remoteFiles.put(file.name.toLowerCase(), file);
        }
        return remoteFiles;
    }

    private String computeLocalHash(final File clientJar)
            throws FileNotFoundException, IOException {
        if (clientJar == null) {
            throw new NullPointerException("clientJar");
        }
        try (ZipFile zipFile = new ZipFile(clientJar)) {
            ZipEntry manifest = zipFile.getEntry("META-INF/MANIFEST.MF");
            if (manifest == null) {
                return "<none>";
            }
            try (final InputStream is = zipFile.getInputStream(manifest)) {
                try (final DigestInputStream dis = new DigestInputStream(is, digest)) {
                    while (dis.read(ioBuffer) != -1) {
                        // DigestInputStream is doing its job, we just need to read through it.
                    }
                }
            }
        }
        final byte[] localHashBytes = digest.digest();
        final String hashString = new BigInteger(1, localHashBytes).toString(16);
        return padLeft(hashString, 40);
    }

    private static String padLeft(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int toPrepend = n - s.length(); toPrepend > 0; toPrepend--) {
            sb.append('0');
        }
        sb.append(s);
        return sb.toString();
    }

    private static FileToDownload pickNativeDownload() {
        final String osName;
        switch (OperatingSystem.detect()) {
            case WINDOWS:
                osName = "windows";
                break;
            case MACOS:
                osName = "macosx";
                break;
            case NIX:
                osName = "linux";
                break;
            case SOLARIS:
                osName = "solaris";
                break;
            default:
                throw new IllegalArgumentException();
        }
        final String remoteName = osName + "_natives.jar";
        final File localPath = new File(PathUtil.getClientDir(),
                "natives/" + osName + "_natives.jar");
        return new FileToDownload(remoteName, localPath);
    }

    private File downloadFile(final FileToDownload file)
            throws MalformedURLException, FileNotFoundException, IOException, InterruptedException {
        if (file == null) {
            throw new NullPointerException("file");
        }
        final File tempFile = File.createTempFile(file.localName.getName(), ".downloaded");
        final URL website = new URL(SharedUpdaterCode.BASE_URL + file.remoteUrl);

        try (final InputStream siteIn = website.openStream()) {
            try (final FileOutputStream fileOut = new FileOutputStream(tempFile)) {
                int len;
                int total = 0;
                while ((len = siteIn.read(ioBuffer)) > 0) {
                    fileOut.write(ioBuffer, 0, len);
                    total += len;
                    signalDownloadPercent(total, file.remoteLength);
                }
            }
        }
        return tempFile;
    }

    // =============================================================================================
    //                                                                      POST-DOWNLOAD PROCESSING
    // =============================================================================================
    private void deployFile(final File processedFile, final File localName) {
        if (processedFile == null) {
            throw new NullPointerException("processedFile");
        }
        if (localName == null) {
            throw new NullPointerException("localName");
        }
        LogUtil.getLogger().log(Level.INFO, "deployFile({0})", localName.getName());
        try {
            final File parentDir = localName.getCanonicalFile().getParentFile();
            if (!parentDir.exists() && !parentDir.mkdirs()) {
                throw new IOException("Unable to make directory " + parentDir);
            }
            PathUtil.replaceFile(processedFile, localName);
            if (localName.getName().endsWith("natives.jar")) {
                extractNatives(localName);
            }
        } catch (final IOException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error deploying " + localName.getName(), ex);
        }
    }

    protected void extractNatives(final File jarPath)
            throws FileNotFoundException, IOException {
        if (jarPath == null) {
            throw new NullPointerException("jarPath");
        }
        LogUtil.getLogger().log(Level.FINE, "extractNatives({0})", jarPath.getName());

        File nativeFolder = new File(PathUtil.getClientDir(), "natives");

        if (!nativeFolder.exists() && !nativeFolder.mkdirs()) {
            throw new IOException("Unable to make directory " + nativeFolder);
        }

        try (final JarFile jarFile = new JarFile(jarPath, true)) {
            for (final JarEntry entry : Collections.list(jarFile.entries())) {
                if (!entry.isDirectory() && (entry.getName().indexOf('/') == -1)) {

                    final File outFile = new File(nativeFolder, entry.getName());

                    if (outFile.exists() && !outFile.delete()) {
                        LogUtil.getLogger().log(Level.SEVERE,
                                "Could not replace native file: {0}", entry.getName());
                        continue;
                    }

                    try (final InputStream in = jarFile.getInputStream(entry)) {
                        try (final FileOutputStream out = new FileOutputStream(outFile)) {
                            final byte[] buffer = new byte[65536];
                            int bufferSize;
                            while ((bufferSize = in.read(buffer, 0, buffer.length)) != -1) {
                                out.write(buffer, 0, bufferSize);
                            }
                        }
                    }
                }
            }
        }
    }
    // =============================================================================================
    //                                                                            PROGRESS REPORTING
    // =============================================================================================
    private volatile ClientUpdateScreen updateScreen;
    private int activeFileNumber, totalFiles;
    private FileToDownload activeFile;

    @Override
    protected synchronized void process(final List<ProgressUpdate> chunks) {
        if (chunks == null) {
            throw new NullPointerException("chunks");
        }
        if (this.updateScreen != null) {
            this.updateScreen.setStatus(chunks.get(chunks.size() - 1));
        }
    }

    private void signalCheckProgress(final String fileName) {
        if (fileName == null) {
            throw new NullPointerException("fileName");
        }
        final int overallProgress = -1; // between 0 and 15%
        final String status = "Checking for updates...";
        this.publish(new ProgressUpdate(fileName, status, overallProgress));
    }

    private void signalDownloadProgress() {
        int overallProgress = (this.activeFileNumber * 100) / totalFiles;
        final String fileName = activeFile.localName.getName();
        final String status = "Preparing to download...";
        this.publish(new ProgressUpdate(fileName, status, overallProgress));
    }

    private void signalDownloadPercent(final long bytesSoFar, final long bytesTotal) {
        final String fileName = activeFile.localName.getName();
        final String status;
        final int overallProgress;
        if (bytesTotal > 0) {
            final int percent = (int) Math.max(0, Math.min(100, (bytesSoFar * 100) / bytesTotal));
            final int baseProgress = (this.activeFileNumber * 100) / this.totalFiles;
            final int deltaProgress = 100 / this.totalFiles;
            overallProgress = baseProgress + (deltaProgress * percent) / 100;
            status = String.format("Downloading (%s / %s)", bytesSoFar, bytesTotal);
        } else {
            status = String.format("Downloading... (%s)", bytesSoFar);
            overallProgress = (this.activeFileNumber * 100) / this.totalFiles;
        }
        this.publish(new ProgressUpdate(fileName, status, overallProgress));
    }

    private void signalUnpackProgress() {
        int overallProgress = (this.activeFileNumber * 100 + 100) / this.totalFiles;
        final String fileName = activeFile.localName.getName();
        final String status = "Unpacking...";
        this.publish(new ProgressUpdate(fileName, status, overallProgress));
    }

    private void signalDone() {
        final String message = (this.updatesApplied ? "Updates applied." : "No updates needed.");
        this.publish(new ProgressUpdate(" ", message, 100));
    }

    @Override
    protected synchronized void done() {
        if (this.updateScreen != null) {
            this.signalDone();
            this.updateScreen.onUpdateDone(this.updatesApplied);
        }
    }

    public synchronized void registerUpdateScreen(final ClientUpdateScreen updateScreen) {
        if (updateScreen == null) {
            throw new NullPointerException("updateScreen");
        }
        this.updateScreen = updateScreen;
        if (this.isDone()) {
            this.signalDone();
            updateScreen.onUpdateDone(this.updatesApplied);
        }
    }

    private void verifyFiles(List<FileToDownload> files) {
        for (FileToDownload file : files) {
            if (!file.localName.exists()) {
                throw new RuntimeException("Update process failed: ");
            }
        }
    }

    // =============================================================================================
    //                                                                                   INNER TYPES
    // =============================================================================================
    private final static class FileToDownload {

        public final String remoteUrl;
        public final File localName;
        public long remoteLength;

        public FileToDownload(final String remoteName, final File localName) {
            this.remoteUrl = remoteName;
            this.localName = localName;
        }
    }

    private final static class RemoteFile {

        String name;
        long length;
        String hash;
    }

    public final static class ProgressUpdate {

        public String fileName;
        public String status;
        public int progress;

        public ProgressUpdate(final String action, final String status, final int progress) {
            this.fileName = action;
            this.status = status;
            this.progress = progress;
        }
    }
}