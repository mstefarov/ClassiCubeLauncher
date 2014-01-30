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
import net.classicube.launcher.gui.UpdateScreen;

// Handles downloading and deployment of client updates,
// as well as resource files used by the client.
public final class UpdateTask
        extends SwingWorker<Boolean, UpdateTask.ProgressUpdate> {

    // =============================================================================================
    //                                                                    CONSTANTS & INITIALIZATION
    // =============================================================================================
    private static final int MAX_PARALLEL_DOWNLOADS = 5;
    private static final UpdateTask instance = new UpdateTask();

    public static UpdateTask getInstance() {
        return instance;
    }

    private UpdateTask() {
    }

    // =============================================================================================
    //                                                                                          MAIN
    // =============================================================================================
    private Thread[] workerThreads;
    private final List<FileToDownload> files = new ArrayList<>();
    private int activeFileNumber, filesDone, totalFiles;
    private boolean needLzma;
    private boolean updatesApplied;

    @Override
    protected Boolean doInBackground()
            throws Exception {
        this.digest = MessageDigest.getInstance("SHA1");
        final Logger logger = LogUtil.getLogger();

        // build up file list
        logger.log(Level.INFO, "Checking for updates.");
        files.addAll(pickBinariesToDownload());
        files.addAll(pickResourcesToDownload());

        if (files.isEmpty()) {
            logger.log(Level.INFO, "No updates needed.");

        } else {
            this.updatesApplied = true;
            logger.log(Level.INFO, "Downloading updates: {0}", listFileNames(files));

            this.activeFileNumber = 0;
            this.totalFiles = files.size();

            if (needLzma) {
                // We need to get lzma.jar before deploying any other files, because some of them
                // may need to be decompressed. "lzma.jar" will always be the first on the list.
                processOneFile(getNextFileSync(false));
            }

            // The rest of the files are processed by worker threads.
            int numThreads = Math.min(totalFiles, MAX_PARALLEL_DOWNLOADS);
            workerThreads = new Thread[numThreads];
            for (int i = 0; i < numThreads; i++) {
                workerThreads[i] = new DownloadThread(logger);
                workerThreads[i].start();
            }
            // Wait for all workers to finish
            for (int i = 0; i < numThreads; i++) {
                workerThreads[i].join();
            }
        }

        // confirm that all required files have been downloaded and deployed
        verifyFiles(files);

        if (this.updatesApplied) {
            logger.log(Level.INFO, "Updates applied.");
        }
        return true;
    }

    private void processOneFile(final FileToDownload file)
            throws InterruptedException, IOException {
        // step 1: download
        final File downloadedFile = downloadFile(file);

        // step 2: unpack
        final File processedFile = SharedUpdaterCode.processDownload(
                LogUtil.getLogger(),
                downloadedFile, file.baseUrl + file.remoteName, file.targetName.getName());

        // step 3: deploy
        deployFile(processedFile, file.targetName);
    }

    // Make a list of all local names, for logging
    private static String listFileNames(final List<FileToDownload> files) {
        if (files == null) {
            throw new NullPointerException("files");
        }
        final StringBuilder sb = new StringBuilder();
        String sep = "";
        for (final FileToDownload s : files) {
            sb.append(sep).append(s.localName.getName());
            sep = ", ";
        }
        return sb.toString();
    }

    // Grabs the next file from the list, and sends a progress report to UpdateScreen.
    // Returns null when there are no more files left to download.
    private synchronized FileToDownload getNextFileSync(boolean fileWasDone) {
        if (fileWasDone) {
            filesDone++;
        }
        FileToDownload fileToReturn = null;
        String fileNameToReport;

        if (activeFileNumber != totalFiles) {
            fileToReturn = files.get(activeFileNumber);
            activeFileNumber++;
            fileNameToReport = fileToReturn.localName.getName();
        } else {
            fileNameToReport = files.get(totalFiles - 1).localName.getName();
        }

        int overallProgress = (this.filesDone * 100 + 100) / this.totalFiles;
        final String status = String.format("Updating %s (%d/%d)",
                fileNameToReport, this.activeFileNumber, this.totalFiles);
        this.publish(new ProgressUpdate(status, overallProgress));
        return fileToReturn;
    }

    // =============================================================================================
    //                                                                        CHECKING / DOWNLOADING
    // =============================================================================================
    private MessageDigest digest;
    private final byte[] ioBuffer = new byte[64 * 1024];
    private final static String[] resourceFiles = new String[]{"music/calm1.ogg", "music/calm2.ogg", "music/calm3.ogg",
        "newmusic/hal1.ogg", "newmusic/hal2.ogg", "newmusic/hal3.ogg", "newmusic/hal4.ogg",
        "newsound/step/grass1.ogg", "newsound/step/grass2.ogg", "newsound/step/grass3.ogg",
        "newsound/step/grass4.ogg", "newsound/step/gravel1.ogg",
        "newsound/step/gravel2.ogg", "newsound/step/gravel3.ogg",
        "newsound/step/gravel4.ogg", "newsound/step/stone1.ogg",
        "newsound/step/stone2.ogg", "newsound/step/stone3.ogg", "newsound/step/stone4.ogg",
        "newsound/step/wood1.ogg", "newsound/step/wood2.ogg", "newsound/step/wood3.ogg",
        "newsound/step/wood4.ogg", "newsound/step/cloth1.ogg", "newsound/step/cloth2.ogg",
        "newsound/step/cloth3.ogg", "newsound/step/cloth4.ogg", "newsound/step/sand1.ogg",
        "newsound/step/sand2.ogg", "newsound/step/sand3.ogg", "newsound/step/sand4.ogg",
        "newsound/step/snow1.ogg", "newsound/step/snow2.ogg", "newsound/step/snow3.ogg",
        "newsound/step/snow4.ogg", "sound3/dig/grass1.ogg", "sound3/dig/grass2.ogg",
        "sound3/dig/grass3.ogg", "sound3/dig/grass4.ogg", "sound3/dig/gravel1.ogg",
        "sound3/dig/gravel2.ogg", "sound3/dig/gravel3.ogg", "sound3/dig/gravel4.ogg",
        "sound3/dig/stone1.ogg", "sound3/dig/stone2.ogg", "sound3/dig/stone3.ogg",
        "sound3/dig/stone4.ogg", "sound3/dig/wood1.ogg", "sound3/dig/wood2.ogg",
        "sound3/dig/wood3.ogg", "sound3/dig/wood4.ogg", "sound3/dig/cloth1.ogg",
        "sound3/dig/cloth2.ogg", "sound3/dig/cloth3.ogg", "sound3/dig/cloth4.ogg",
        "sound3/dig/sand1.ogg", "sound3/dig/sand2.ogg", "sound3/dig/sand3.ogg",
        "sound3/dig/sand4.ogg", "sound3/dig/snow1.ogg", "sound3/dig/snow2.ogg",
        "sound3/dig/snow3.ogg", "sound3/dig/snow4.ogg", "sound3/random/glass1.ogg",
        "sound3/random/glass2.ogg", "sound3/random/glass3.ogg"};
    public static final String FILE_INDEX_URL = "http://www.classicube.net/static/client/version",
            RESOURCE_DOWNLOAD_URL = "https://s3.amazonaws.com/MinecraftResources/",
            LAUNCHER_JAR = "launcher.jar";

    private List<FileToDownload> pickResourcesToDownload()
            throws IOException {
        final List<FileToDownload> pickedFiles = new ArrayList<>();

        final File resDir = new File(PathUtil.getClientDir(), "resources");
        for (final String resFileName : resourceFiles) {
            final File resFile = new File(resDir, resFileName);
            if (!resFile.exists()) {
                pickedFiles.add(new FileToDownload(RESOURCE_DOWNLOAD_URL, resFileName, resFile));
            }
        }
        return pickedFiles;
    }

    private List<FileToDownload> pickBinariesToDownload()
            throws IOException {
        final List<FileToDownload> filesToDownload = new ArrayList<>();
        final List<FileToDownload> localFiles = listBinaries();
        final HashMap<String, RemoteFile> remoteFiles = getRemoteIndex();
        final boolean updateExistingFiles = (Prefs.getUpdateMode() != UpdateMode.DISABLED);

        // Getting remote file index failed. Abort update.
        if (remoteFiles == null) {
            return filesToDownload;
        }

        for (final FileToDownload localFile : localFiles) {
            signalCheckProgress(localFile.localName.getName());
            
            final RemoteFile remoteFile = remoteFiles.get(localFile.remoteName);
            boolean download = false;
            boolean localFileMissing = !localFile.localName.exists();
            File fileToHash = localFile.localName;
            
            // lzma.jar and launcher.jar get special treatment
            boolean isLzma = (localFile == lzmaJarFile);
            boolean isLauncherJar = (localFile == launcherJarFile);

            if (isLauncherJar) {
                if (localFileMissing) {
                    // If launcher.jar is missing from its usual location, that means we're
                    // currently running from somewhere else. We need to take care to avoid
                    // repeated attempts to update the launcher.
                    LogUtil.getLogger().log(Level.WARNING,
                            "launcher.jar is not present in its usual location!");
                    // We check if "launcher.jar.new" is up-to-date (instead of checking "launcher.jar"),
                    // and only download it if UpdateMode is not DISABLED.
                    fileToHash = localFile.targetName;
                    localFileMissing = !localFile.targetName.exists();
                } else if (localFile.targetName.exists()) {
                    // If "launcher.jar.new" already exists, just check if it's up-to-date.
                    fileToHash = localFile.targetName;
                    LogUtil.getLogger().log(Level.WARNING,
                            "launcher.jar.new already exists: we're probably not running from self-updater.");
                }
            }

            if (localFileMissing) {
                // If local file does not exist
                LogUtil.getLogger().log(Level.INFO,
                        "Will download {0}: does not exist locally", localFile.localName.getName());
                download = true;

            } else if (updateExistingFiles && !isLzma) {
                // If local file exists, but may need updating
                if (remoteFile != null) {
                    try {
                        final String localHash = computeLocalHash(fileToHash);
                        if (!localHash.equalsIgnoreCase(remoteFile.hash)) {
                            // If file contents don't match
                            LogUtil.getLogger().log(Level.INFO,
                                    "Will download {0}: contents don''t match ({1} vs {2})",
                                    new Object[]{fileToHash.getName(), localHash, remoteFile.hash});
                            download = true;
                        }
                    } catch (final IOException ex) {
                        LogUtil.getLogger().log(Level.SEVERE,
                                "Error computing hash of a local file", ex);
                    }
                } else {
                    LogUtil.getLogger().log(Level.WARNING,
                            "No remote match for local file {0}", fileToHash.getName());
                }
            }

            if (download) {
                if (isLzma) {
                    needLzma = true;
                } else if (remoteFile == null) {
                    String errMsg = String.format("Required file \"%s%s\" cannot be found.",
                            localFile.baseUrl, localFile.remoteName);
                    throw new RuntimeException(errMsg);
                }
                filesToDownload.add(localFile);
            }
        }
        return filesToDownload;
    }

    private FileToDownload lzmaJarFile, launcherJarFile;

    private List<FileToDownload> listBinaries()
            throws IOException {
        final List<FileToDownload> binaryFiles = new ArrayList<>();

        final File clientDir = PathUtil.getClientDir();
        final File launcherDir = SharedUpdaterCode.getLauncherDir();

        lzmaJarFile = new FileToDownload(SharedUpdaterCode.BASE_URL, "lzma.jar",
                new File(launcherDir, "lzma.jar"));
        binaryFiles.add(lzmaJarFile);

        launcherJarFile = new FileToDownload(SharedUpdaterCode.BASE_URL, "launcher.jar.pack.lzma",
                new File(launcherDir, LAUNCHER_JAR),
                new File(launcherDir, SharedUpdaterCode.LAUNCHER_NEW_JAR_NAME));
        binaryFiles.add(launcherJarFile);

        binaryFiles.add(new FileToDownload(SharedUpdaterCode.BASE_URL, "client.jar.pack.lzma",
                new File(clientDir, "client.jar")));

        binaryFiles.add(new FileToDownload(SharedUpdaterCode.BASE_URL, "lwjgl.jar.pack.lzma",
                new File(clientDir, "libs/lwjgl.jar")));
        binaryFiles.add(new FileToDownload(SharedUpdaterCode.BASE_URL, "lwjgl_util.jar.pack.lzma",
                new File(clientDir, "libs/lwjgl_util.jar")));
        binaryFiles.add(new FileToDownload(SharedUpdaterCode.BASE_URL, "jinput.jar.pack.lzma",
                new File(clientDir, "libs/jinput.jar")));
        binaryFiles.add(pickNativeDownload());

        return binaryFiles;
    }

    // get a list of files available from CC.net
    private HashMap<String, RemoteFile> getRemoteIndex() {
        final String hashIndex = HttpUtil.downloadString(FILE_INDEX_URL);
        final HashMap<String, RemoteFile> remoteFiles = new HashMap<>();

        // if getting the list failed, don't panic. Abort update instead.
        if (hashIndex == null) {
            return null;
        }

        // special treatment for LZMA
        final RemoteFile lzmaFile = new RemoteFile();
        lzmaFile.name = SharedUpdaterCode.LZMA_JAR_NAME;
        lzmaFile.hash = "N/A";
        remoteFiles.put(lzmaFile.name.toLowerCase(), lzmaFile);

        // the rest of the files
        for (final String line : hashIndex.split("\\r?\\n")) {
            final String[] components = line.split(" ");
            final RemoteFile file = new RemoteFile();
            file.name = components[0];
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
        try (final ZipFile zipFile = new ZipFile(clientJar)) {
            final ZipEntry manifest = zipFile.getEntry("META-INF/MANIFEST.MF");
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
        return padLeft(hashString, '0', 40);
    }

    private static String padLeft(final String s, final char c, final int n) {
        if (s == null) {
            throw new NullPointerException("s");
        }
        final StringBuilder sb = new StringBuilder();
        for (int toPrepend = n - s.length(); toPrepend > 0; toPrepend--) {
            sb.append(c);
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
        return new FileToDownload(SharedUpdaterCode.BASE_URL, remoteName, localPath);
    }

    private File downloadFile(final FileToDownload file)
            throws MalformedURLException, FileNotFoundException, IOException, InterruptedException {
        if (file == null) {
            throw new NullPointerException("file");
        }
        final File tempFile = File.createTempFile(file.localName.getName(), ".downloaded");
        final URL website = new URL(file.baseUrl + file.remoteName);

        try (final InputStream siteIn = website.openStream()) {
            try (final FileOutputStream fileOut = new FileOutputStream(tempFile)) {
                int len;
                while ((len = siteIn.read(ioBuffer)) > 0) {
                    fileOut.write(ioBuffer, 0, len);
                }
            }
        }
        return tempFile;
    }

    // =============================================================================================
    //                                                                      POST-DOWNLOAD PROCESSING
    // =============================================================================================
    private void deployFile(final File processedFile, File targetFile) {
        if (processedFile == null) {
            throw new NullPointerException("processedFile");
        }
        if (targetFile == null) {
            throw new NullPointerException("localName");
        }
        LogUtil.getLogger().log(Level.INFO, "Deploying {0}", targetFile);
        try {
            final File parentDir = targetFile.getCanonicalFile().getParentFile();
            if (!parentDir.exists() && !parentDir.mkdirs()) {
                throw new IOException("Unable to make directory " + parentDir);
            }
            PathUtil.replaceFile(processedFile, targetFile);

            // special handling for natives
            if (targetFile.getName().endsWith("natives.jar")) {
                extractNatives(targetFile);
            }
        } catch (final IOException ex) {
            LogUtil.getLogger().log(Level.SEVERE, "Error deploying " + targetFile.getName(), ex);
        }
    }

    protected void extractNatives(final File jarPath)
            throws FileNotFoundException, IOException {
        if (jarPath == null) {
            throw new NullPointerException("jarPath");
        }
        LogUtil.getLogger().log(Level.FINE, "extractNatives({0})", jarPath.getName());

        final File nativeFolder = new File(PathUtil.getClientDir(), "natives");

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
    private volatile UpdateScreen updateScreen;

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
        this.publish(new ProgressUpdate("Checking " + fileName, -1));
    }

    private void signalDone() {
        final String message = (this.updatesApplied ? "Updates applied." : "No updates needed.");
        this.publish(new ProgressUpdate(message, 100));
    }

    @Override
    protected synchronized void done() {
        if (this.updateScreen != null) {
            this.signalDone();
            this.updateScreen.onUpdateDone(this.updatesApplied);
        }
    }

    public synchronized void registerUpdateScreen(final UpdateScreen updateScreen) {
        if (updateScreen == null) {
            throw new NullPointerException("updateScreen");
        }
        this.updateScreen = updateScreen;
        if (this.isDone()) {
            this.signalDone();
            updateScreen.onUpdateDone(this.updatesApplied);
        }
    }

    private void verifyFiles(final List<FileToDownload> files) {
        if (files == null) {
            throw new NullPointerException("files");
        }
        for (final FileToDownload file : files) {
            if (!LAUNCHER_JAR.equals(file.localName.getName()) && !file.localName.exists()) {
                throw new RuntimeException("Update process failed. Missing file: " + file.localName);
            }
        }
    }

    // =============================================================================================
    //                                                                                   INNER TYPES
    // =============================================================================================
    public final static class ProgressUpdate {

        public String statusString;
        public int progress;

        public ProgressUpdate(final String statusString, final int progress) {
            if (statusString == null) {
                throw new NullPointerException("statusString");
            }
            this.statusString = statusString;
            this.progress = progress;
        }
    }

    private final static class FileToDownload {

        // remote filename
        public final String baseUrl;
        public final String remoteName;
        public final File localName;
        public final File targetName;

        public FileToDownload(final String baseUrl, final String remoteName, final File localName) {
            this(baseUrl, remoteName, localName, localName);
        }

        public FileToDownload(final String baseUrl, final String remoteName, final File localName, final File targetName) {
            this.baseUrl = baseUrl;
            this.remoteName = remoteName;
            this.localName = localName;
            this.targetName = targetName;
        }
    }

    private final static class RemoteFile {

        String name;
        String hash;
    }

    private class DownloadThread extends Thread {

        private final Logger logger;

        public DownloadThread(Logger logger) {
            this.logger = logger;
        }

        @Override
        public void run() {
            try {
                FileToDownload file = getNextFileSync(false);
                while (file != null) {
                    processOneFile(file);
                    file = getNextFileSync(true);
                }

            } catch (final IOException | InterruptedException ex) {
                logger.log(Level.SEVERE, "Error downloading an updated file.", ex);
            }
        }
    }
}
