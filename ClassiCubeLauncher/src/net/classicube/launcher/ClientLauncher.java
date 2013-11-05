package net.classicube.launcher;

import net.classicube.launcher.gui.ErrorScreen;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.classicube.launcher.gui.DebugWindow;

// Handles launching the client process.
final public class ClientLauncher {

    private static final String ClassPath = "client.jar" + File.pathSeparatorChar + "libs/*",
            ClientClassPath = "com.oyasunadev.mcraft.client.core.ClassiCubeStandalone";

    public static void launchClient(final ServerJoinInfo joinInfo) {
        if (joinInfo == null) {
            throw new NullPointerException("joinInfo");
        }
        LogUtil.getLogger().info("launchClient");
        SessionManager.getSession().storeResumeInfo(joinInfo);

        final File java = PathUtil.getJavaPath();

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
            LogUtil.getLogger().log(Level.INFO, concatStringsWSep(processBuilder.command(), " "));

            if (Prefs.getDebugMode()) {
                processBuilder.redirectErrorStream(true);
                try {
                    final Process p = processBuilder.start();

                    // capture stdin, redirect to stdout
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                Scanner input = new Scanner(p.getInputStream());
                                while (true) {
                                    DebugWindow.WriteLine(input.nextLine());
                                }
                            } catch (NoSuchElementException ex) {
                                DebugWindow.WriteLine("(client closed)");
                            }
                        }
                    }.start();

                } catch (IOException ex) {
                    LogUtil.getLogger().log(Level.SEVERE, "Error launching client", ex);
                }
            } else {
                processBuilder.start();
                System.exit(0);
            }

        } catch (final Exception ex) {
            ErrorScreen.show(null, "Could not launch the game",
                    "Error launching the client:<br>" + ex.getMessage(), ex);
        }
    }

    private static String concatStringsWSep(final List<String> strings, final String separator) {
        if (strings == null) {
            throw new NullPointerException("strings");
        }
        if (separator == null) {
            throw new NullPointerException("separator");
        }
        final StringBuilder sb = new StringBuilder();
        String sep = "";
        for (final String s : strings) {
            sb.append(sep).append(s);
            sep = separator;
        }
        return sb.toString();
    }
}
