package net.classicube.launcher;

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

final class ClientUpdateTask
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

        return true;
    }

    private List<FileToDownload> findFilesToDownload() {
        final List<FileToDownload> files = new ArrayList<>();

        final File clientDir = PathUtil.getClientDir();
        final File launcherDir = PathUtil.getLauncherDir();

        files.add(new FileToDownload(
                "lzma.jar",
                new File(launcherDir, "lzma.jar")));

        /*
         files.add(new FileToDownload(
         "launcher.jar",
         new File(launcherDir, "ClassiCubeLauncher.jar.new")));
         */ // TODO: auto-update launcher when we start regular deployment

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

    private List<FileToDownload> pickFilesToDownload() {
        List<FileToDownload> filesToDownload = new ArrayList<>();
        List<FileToDownload> localFiles = findFilesToDownload();
        HashMap<String, RemoteFile> remoteFiles = getRemoteIndex();

        for (FileToDownload localFile : localFiles) {
            signalCheckProgress(localFile.localName.getName());
            RemoteFile remoteFile = remoteFiles.get(localFile.remoteUrl);
            if (remoteFile != null) {
                boolean isLzma = localFile.localName.getName().equals(PathUtil.LZMA_JAR_NAME);
                boolean download = false;
                if (!localFile.localName.exists()) {
                    // If local file does not exist
                    LogUtil.getLogger().log(Level.INFO,
                            "ClientUpdateTask: Will download {0}: does not exist locally",
                            localFile.localName.getName());
                    download = true;
                } else if (!isLzma) {
                    try {
                        String localHash = computeLocalHash(localFile.localName);
                        if (!localHash.equalsIgnoreCase(remoteFile.hash)) {
                            // If file contents don't match
                            LogUtil.getLogger().log(Level.INFO,
                                    "Will download {0}: contents don''t match ({1} vs {2})",
                                    new Object[]{localFile.localName.getName(), localHash, remoteFile.hash});
                            download = true;
                        } else {
                            LogUtil.getLogger().log(Level.INFO,
                                    "Skipping {0}: contents match ({1} = {2})",
                                    new Object[]{localFile.localName.getName(), localHash, remoteFile.hash});
                        }
                    } catch (IOException ex) {
                        LogUtil.getLogger().log(Level.SEVERE,
                                "Error computing hash of a local file", ex);
                    }
                }
                if (download) {
                    localFile.remoteLength = remoteFile.length;
                    filesToDownload.add(localFile);
                }
            } else {
                LogUtil.getLogger().log(Level.WARNING,
                        "No remote match for local file {0}", localFile.localName.getName());
            }
        }
        return filesToDownload;
    }

    private HashMap<String, RemoteFile> getRemoteIndex() {
        String hashIndex = HttpUtil.downloadString("http://www.classicube.net/static/client/version");
        HashMap<String, RemoteFile> remoteFiles = new HashMap<>();

        // special treatment for LZMA
        RemoteFile lzmaFile = new RemoteFile();
        lzmaFile.name = PathUtil.LZMA_JAR_NAME;
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
                final DigestInputStream dis = new DigestInputStream(is, digest);
                while (dis.read(ioBuffer) != -1) {
                    // DigestInputStream is doing its job, we just need to read through it.
                }
            }
        }
        final byte[] localHashBytes = digest.digest();
        return new BigInteger(1, localHashBytes).toString(16);
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
        final String remoteName = osName + "_natives.jar.pack.lzma";
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
            if (!parentDir.exists()) {
                parentDir.mkdirs();
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

        if (!nativeFolder.exists()) {
            nativeFolder.mkdir();
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
        final String status = String.format("Checking for updates...", fileName);
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
        final String status = String.format("Unpacking...", fileName);
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