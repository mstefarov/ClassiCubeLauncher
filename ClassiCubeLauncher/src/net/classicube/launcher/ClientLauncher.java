package net.classicube.launcher;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Level;
import net.classicube.launcher.gui.DebugWindow;
import net.classicube.launcher.gui.ErrorScreen;

// Handles launching the client process.
final public class ClientLauncher {

    private static final String ClassPath = "client.jar" + File.pathSeparatorChar + "libs/*",
            ClientClassPath = "com.oyasunadev.mcraft.client.core.ClassiCubeStandalone";

    public static void launchClient(final ServerJoinInfo joinInfo) {
        LogUtil.getLogger().info("launchClient");

        if (joinInfo != null) {
            SessionManager.getSession().storeResumeInfo(joinInfo);
        }// else if joinInfo==null, then we're launching singleplayer

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
            final ProcessBuilder processBuilder;
            if (joinInfo != null) {
                processBuilder = new ProcessBuilder(
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
                        (joinInfo.pass == null || joinInfo.pass.length() == 0 ? "none" : joinInfo.pass),
                        SessionManager.getSession().getSkinUrl(),
                        Boolean.toString(Prefs.getFullscreen()));
            } else {
                processBuilder = new ProcessBuilder(
                        java.getAbsolutePath(),
                        "-cp",
                        ClassPath,
                        "-Djava.library.path=" + nativePath,
                        Prefs.getJavaArgs(),
                        "-Xmx" + Prefs.getMaxMemory() + "m",
                        ClientClassPath,
                        "none",
                        "0",
                        "none",
                        "none",
                        SessionManager.getSession().getSkinUrl(),
                        Boolean.toString(Prefs.getFullscreen()));
            }

            processBuilder.directory(PathUtil.getClientDir());

            // log the command used to launch client
            String cmdLineToLog = concatStringsWSep(processBuilder.command(), " ");
            if (joinInfo != null) {
                cmdLineToLog = cmdLineToLog.replace(joinInfo.pass, "########"); // sanitize mppass
            }
            LogUtil.getLogger().log(Level.INFO, cmdLineToLog);

            if (Prefs.getDebugMode()) {
                processBuilder.redirectErrorStream(true);
                try {
                    final Process p = processBuilder.start();
                    DebugWindow.setWindowTitle("Game Running");

                    // capture output from the client, redirect to DebugWindow
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                Scanner input = new Scanner(p.getInputStream());
                                while (true) {
                                    DebugWindow.writeLine(input.nextLine());
                                }
                            } catch (NoSuchElementException ex) {
                                DebugWindow.writeLine("(client closed)");
                                DebugWindow.setWindowTitle("Client Closed");
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
