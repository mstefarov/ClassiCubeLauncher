package net.classicube.launcher;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

// Handles launching the client.
final public class ClientLauncher {

    private static final String ClassPath = "client.jar" + File.pathSeparatorChar + "libs/*",
            ClientClassPath = "com.oyasunadev.mcraft.client.core.ClassiCubeStandalone";

    public static void launchClient(ServerJoinInfo joinInfo) {
        LogUtil.getLogger().info("launchClient");
        SessionManager.getSession().storeResumeInfo(joinInfo);

        final File java = getJavaPath();

        final String nativePath;
        try {
            nativePath = new File(PathUtil.getClientDir(), "natives").getCanonicalPath();
        } catch (final Exception ex) {
            ErrorScreen.show(null, "Could not launch the game",
                    "Error finding the LWJGL native library path:<br>" + ex.getMessage(), ex);
            return;
        }

        try {
            final ProcessBuilder processBuilder = new ProcessBuilder(
                    java.getAbsolutePath(),
                    "-cp",
                    ClassPath,
                    "-Djava.library.path=" + nativePath,
                    Prefs.getJavaArgs(),
                    "-Xmx" + Prefs.getMaxMemory() + "m",
                    ClientClassPath,
                    joinInfo.address.getHostAddress(),
                    Integer.toString(joinInfo.port),
                    joinInfo.playerName,
                    joinInfo.pass,
                    SessionManager.getSession().getSkinUrl(),
                    Boolean.toString(Prefs.getFullscreen()));
            processBuilder.directory(PathUtil.getClientDir());
            //processBuilder.inheritIO();

            LogUtil.getLogger().log(Level.INFO, concatStringsWSep(processBuilder.command(), " "));
            Process p = processBuilder.start();
            //p.waitFor();
            System.exit(0);
        } catch (final Exception ex) {
            ErrorScreen.show(null, "Could not launch the game",
                    "Error launching the client:<br>" + ex.getMessage(), ex);
        }
    }

    private static String concatStringsWSep(final List<String> strings, final String separator) {
        final StringBuilder sb = new StringBuilder();
        String sep = "";
        for (String s : strings) {
            sb.append(sep).append(s);
            sep = separator;
        }
        return sb.toString();
    }

    private static File getJavaPath() {
        return new File(System.getProperty("java.home"), "bin/java");
    }
}
