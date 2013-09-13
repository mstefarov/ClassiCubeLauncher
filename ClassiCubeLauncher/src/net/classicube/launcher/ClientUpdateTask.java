package net.classicube.launcher;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
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
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Unpacker;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import lzma.sdk.lzma.Decoder;
import lzma.streams.LzmaInputStream;

final class ClientUpdateTask
        extends SwingWorker<Boolean, ClientUpdateTask.ProgressUpdate> {

    // =============================================================================================
    //                                                                    CONSTANTS & INITIALIZATION
    // =============================================================================================
    private static final String BASE_URL = "http://www.classicube.net/static/client/",
            CLIENT_HASH_URL = BASE_URL + "client.jar.md5",
            LAUNCHER_HASH_URL = BASE_URL + "ClassiCubeLauncher.jar.md5";
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
    private final List<FileToDownload> files = new ArrayList<>();

    @Override
    protected Boolean doInBackground()
            throws Exception {
        if (Prefs.getUpdateMode() == UpdateMode.DISABLED) {
            return true;
        }

        digest = MessageDigest.getInstance("MD5");
        Logger logger = LogUtil.getLogger();

        // step 1: build up file list
        logger.log(Level.INFO, "Checking for updates.");
        findFilesToDownload();

        if (files.isEmpty()) {
            logger.log(Level.INFO, "No updates needed.");

        } else {
            updatesApplied = true;
            logger.log(Level.INFO, "Downloading updates: {0}", listFileNames(files));

            activeFile = 0;
            for (final FileToDownload file : files) {
                try {
                    // step 2: download
                    signalDownloadProgress();
                    final File downloadedFile = downloadFile(file);

                    // step 3: unpack
                    signalUnpackProgress();
                    final File processedFile = processDownload(downloadedFile, file);

                    // step 4: deploy
                    deployFile(processedFile, file.localName);

                } catch (final IOException ex) {
                    logger.log(Level.SEVERE, "Error while downloading update.", ex);
                }
                activeFile++;
            }
            logger.log(Level.INFO, "Updates applied.");
        }

        return true;
    }

    private void findFilesToDownload() {
        final File clientDir = PathUtil.getClientDir();
        final File launcherDir = PathUtil.getLauncherDir();

        if (checkForLauncherUpdate()) {
            files.add(new FileToDownload(
                    BASE_URL + "ClassiCubeLauncher.jar",
                    new File(launcherDir, "ClassiCubeLauncher.jar.new")));
        }
        if (checkForClientUpdate()) {
            files.add(new FileToDownload(
                    BASE_URL + "client.jar",
                    new File(clientDir, "client.jar")));
        }
        if (checkForLibraries()) {
            files.add(new FileToDownload(
                    BASE_URL + "lwjgl.jar.pack.lzma",
                    new File(clientDir, "libs/lwjgl.jar")));
            files.add(new FileToDownload(
                    BASE_URL + "lwjgl_util.jar.pack.lzma",
                    new File(clientDir, "libs/lwjgl_util.jar")));
            files.add(new FileToDownload(
                    BASE_URL + "jinput.jar.pack.lzma",
                    new File(clientDir, "libs/jinput.jar")));
            files.add(pickNativeDownload());
        }
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

    private boolean checkForLauncherUpdate() {
        signalCheckProgress(0, "launcher");
        final File launcherFile = new File(PathUtil.getLauncherDir(), "launcher.jar");
        return checkUpdateByHash(launcherFile, LAUNCHER_HASH_URL);
    }

    private boolean checkForClientUpdate() {
        signalCheckProgress(1, "client");
        final File clientFile = new File(PathUtil.getClientDir(), "client.jar");
        return checkUpdateByHash(clientFile, CLIENT_HASH_URL);
    }

    private boolean checkUpdateByHash(final File localFile, final String hashUrl) {
        if (localFile == null) {
            throw new NullPointerException("localFile");
        }
        if (hashUrl == null) {
            throw new NullPointerException("hashUrl");
        }
        final String name = localFile.getName();
        if (!localFile.exists()) {
            LogUtil.getLogger().log(Level.FINE, "{0}: No local copy, will download.", name);
            // if local file does not exist, always update/download
            return true;

        } else {
            // else check if remote hash is different from local hash
            final String remoteString = HttpUtil.downloadString(hashUrl);
            final String remoteHash = remoteString.substring(0, 32);
            if (remoteHash == null) {
                LogUtil.getLogger().log(Level.FINE,
                        "{0}: Error downloading remote hash, aborting.", name);
                return false; // remote server is down, dont try to update
            } else {
                try {
                    final String localHashString = computeLocalHash(localFile);
                    return !localHashString.equalsIgnoreCase(remoteHash);
                } catch (final IOException ex) {
                    LogUtil.getLogger().log(Level.SEVERE,
                            name + ": Error computing local hash, aborting.", ex);
                    return false;
                }
            }
        }
    }

    private String computeLocalHash(final File clientJar)
            throws FileNotFoundException, IOException {
        if (clientJar == null) {
            throw new NullPointerException("clientJar");
        }
        try (final FileInputStream is = new FileInputStream(clientJar)) {
            final DigestInputStream dis = new DigestInputStream(is, digest);
            while (dis.read(ioBuffer) != -1) {
                // DigestInputStream is doing its job, we just need to read through it.
            }
        }
        final byte[] localHashBytes = digest.digest();
        return new BigInteger(1, localHashBytes).toString(16);
    }

    private boolean checkForLibraries() {
        signalCheckProgress(2, "library");
        final File libDir = new File(PathUtil.getClientDir(), "libs");
        final FileToDownload nativeLib = pickNativeDownload();
        final File libLwjgl = new File(libDir, "lwjgl.jar");
        final File libLwjglUtil = new File(libDir, "lwjgl_util.jar");
        final File libJInput = new File(libDir, "jinput.jar");
        return !libDir.exists()
                || !nativeLib.localName.exists()
                || !libLwjgl.exists()
                || !libLwjglUtil.exists()
                || !libJInput.exists();
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
        final String remoteName = BASE_URL + osName + "_natives.jar.lzma";
        final File localPath = new File(PathUtil.getClientDir(),
                "natives/" + osName + "_natives.jar");
        return new FileToDownload(remoteName, localPath);
    }

    private File downloadFile(final FileToDownload file)
            throws MalformedURLException, FileNotFoundException, IOException {
        if (file == null) {
            throw new NullPointerException("file");
        }
        final File tempFile = File.createTempFile(file.localName.getName(), ".downloaded");
        final URL website = new URL(file.remoteUrl);
        final int fileSize = website.openConnection().getContentLength(); // TODO: work around lack of content-length

        try (final InputStream siteIn = website.openStream()) {
            try (final FileOutputStream fileOut = new FileOutputStream(tempFile)) {
                int len;
                int total = 0;
                while ((len = siteIn.read(ioBuffer)) > 0) {
                    fileOut.write(ioBuffer, 0, len);
                    total += len;
                    signalDownloadPercent(total, fileSize);
                }
            }
        }
        return tempFile;
    }

    // =============================================================================================
    //                                                                      POST-DOWNLOAD PROCESSING
    // =============================================================================================
    private File processDownload(final File rawFile, final FileToDownload fileInfo)
            throws FileNotFoundException, IOException {
        if (rawFile == null) {
            throw new NullPointerException("rawFile");
        }
        if (fileInfo == null) {
            throw new NullPointerException("fileInfo");
        }
        LogUtil.getLogger().log(Level.FINE, "processDownload({0})", fileInfo.localName.getName());
        final String remoteUrlLower = fileInfo.remoteUrl.toLowerCase();
        final String namePart = fileInfo.localName.getName();

        if (remoteUrlLower.endsWith(".pack.lzma")) {
            // decompress (LZMA) and then unpack (Pack200)
            final File newFile1 = File.createTempFile(namePart, ".decompressed.tmp");
            decompressLzma(rawFile, newFile1);
            rawFile.delete();
            final File newFile2 = File.createTempFile(namePart, ".unpacked.tmp");
            unpack200(newFile1, newFile2);
            newFile1.delete();
            return newFile2;

        } else if (remoteUrlLower.endsWith(".lzma")) {
            // decompress (LZMA)
            final File newFile = File.createTempFile(namePart, ".decompressed.tmp");
            decompressLzma(rawFile, newFile);
            rawFile.delete();
            return newFile;

        } else if (remoteUrlLower.endsWith(".pack")) {
            // unpack (Pack200)
            final File newFile = File.createTempFile(namePart, ".unpacked.tmp");
            unpack200(rawFile, newFile);
            rawFile.delete();
            return newFile;

        } else {
            return rawFile;
        }
    }

    private void decompressLzma(final File compressedInput, final File decompressedOutput)
            throws FileNotFoundException, IOException {
        if (compressedInput == null) {
            throw new NullPointerException("compressedInput");
        }
        if (decompressedOutput == null) {
            throw new NullPointerException("decompressedOutput");
        }
        LogUtil.getLogger().log(Level.FINE, "LZMA: {0}", compressedInput.getName());
        try (final FileInputStream fileIn = new FileInputStream(compressedInput)) {
            try (final BufferedInputStream bufferedIn = new BufferedInputStream(fileIn)) {
                final LzmaInputStream compressedIn = new LzmaInputStream(bufferedIn, new Decoder());
                try (final FileOutputStream fileOut = new FileOutputStream(decompressedOutput)) {
                    int len;
                    while ((len = compressedIn.read(ioBuffer)) > 0) {
                        fileOut.write(ioBuffer, 0, len);
                    }
                }
            }
        }
    }

    private static void unpack200(final File compressedInput, final File decompressedOutput)
            throws FileNotFoundException, IOException {
        if (compressedInput == null) {
            throw new NullPointerException("compressedInput");
        }
        if (decompressedOutput == null) {
            throw new NullPointerException("decompressedOutput");
        }
        LogUtil.getLogger().log(Level.FINE, "unpack200: {0}", compressedInput.getName());
        try (final FileOutputStream fostream = new FileOutputStream(decompressedOutput)) {
            try (final JarOutputStream jostream = new JarOutputStream(fostream)) {
                final Unpacker unpacker = Pack200.newUnpacker();
                unpacker.unpack(compressedInput, jostream);
            }
        }
    }

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
    private int activeFile;

    @Override
    protected synchronized void process(final List<ProgressUpdate> chunks) {
        if (chunks == null) {
            throw new NullPointerException("chunks");
        }
        if (this.updateScreen != null) {
            this.updateScreen.setStatus(chunks.get(chunks.size() - 1));
        }
    }

    private void signalCheckProgress(final int step, final String fileName) {
        if (fileName == null) {
            throw new NullPointerException("fileName");
        }
        final int overallProgress = step * 5; // between 0 and 15%
        final String status = String.format("Checking for updates...", fileName);
        this.publish(new ProgressUpdate(fileName, status, overallProgress));
    }

    private void signalDownloadProgress() {
        int overallProgress = 15 + (this.activeFile * 170) / (this.files.size() * 2);
        final String fileName = this.files.get(this.activeFile).localName.getName();
        final String status = "Downloading...";
        this.publish(new ProgressUpdate(fileName, status, overallProgress));
    }

    private void signalDownloadPercent(final int bytesSoFar, final int bytesTotal) {
        final String fileName = this.files.get(this.activeFile).localName.getName();
        final int totalFiles = this.files.size();
        final String status;
        final int overallProgress;
        if (bytesTotal > 0 && bytesTotal < bytesSoFar) {
            final int percent = Math.max(0, Math.min(100, (bytesSoFar * 100) / bytesTotal));
            final int baseProgress = 15 + (this.activeFile * 170) / (totalFiles * 2);
            final int deltaProgress = 15 + ((this.activeFile + 1) * 170) / (totalFiles * 2) - baseProgress;
            overallProgress = baseProgress + (deltaProgress * percent) / 100;
            status = String.format("Downloading (%s / %s)", bytesSoFar, bytesTotal);
        } else {
            status = String.format("Downloading... (%s)", bytesSoFar);
            overallProgress = 15 + (this.activeFile * 170) / (totalFiles * 2);
        }
        this.publish(new ProgressUpdate(fileName, status, overallProgress));
    }

    private void signalUnpackProgress() {
        int overallProgress = 15 + ((this.activeFile + 1) * 170) / (this.files.size() * 2);
        final String fileName = this.files.get(this.activeFile).localName.getName();
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

        public FileToDownload(final String remoteName, final File localName) {
            this.remoteUrl = remoteName;
            this.localName = localName;
        }
    }

    public final class ProgressUpdate {
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